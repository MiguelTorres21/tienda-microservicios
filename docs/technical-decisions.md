# Decisiones Técnicas

## Base de datos: una por microservicio

Cada servicio tiene su propia instancia de PostgreSQL (`products-db` en `:5432`,
`inventory-db` en `:5433`). No hay acceso cruzado a tablas entre servicios.

**Por qué:** aislamiento de dominio — `inventory-service` no puede leer ni escribir
en las tablas de `products-service` por accidente ni por diseño. Esto permite
escalar, migrar o reemplazar cada base de datos de forma independiente, y evita
el acoplamiento implícito que generaría una base compartida.

**Tradeoff aceptado:** joins entre entidades de distintos dominios requieren una
llamada HTTP. En este caso el único join necesario es "validar que el producto
existe", que se resuelve con una llamada ligera al endpoint interno de
`products-service`.

---

## Concurrencia en compras: optimistic locking + retry

La entidad `Inventory` tiene un campo `version BIGINT` gestionado por
`@Version` de JPA. Cuando dos compras simultáneas leen el mismo registro y
ambas intentan guardarlo, la segunda recibe `ObjectOptimisticLockingFailureException`.

```
T1: lee version=5, descuenta, guarda version=6 → OK
T2: lee version=5, descuenta, intenta guardar version=6 → FALLA (ya es 6)
T2: reintenta — lee version=6, descuenta, guarda version=7 → OK
```

El componente `PurchaseExecutor` usa `@Retryable(retryFor =
ObjectOptimisticLockingFailureException.class, maxAttempts = 3)` con backoff
exponencial de 50 ms. Si los tres intentos fallan, `@Recover` lanza
`ConcurrentPurchaseException` → 409.

**Por qué optimistic y no pessimistic (`SELECT FOR UPDATE`):**
el escenario esperado es baja contención por producto. El locking pesimista
bloquea la fila durante toda la transacción, lo que degrada el throughput
cuando hay muchas compras de productos distintos. Con optimistic locking la fila
nunca se bloquea; solo hay coste cuando realmente hay colisión.

**Garantía:** la constraint `CHECK (available >= 0)` en la DDL es la última
línea de defensa — aunque un bug bypasee la lógica de aplicación, PostgreSQL
rechazará cualquier `available` negativo.

---

## Idempotencia: persistida en base de datos

El endpoint `POST /api/v1/purchases` requiere el header `Idempotency-Key`. La
llave se persiste en la tabla `idempotency_records` con tres estados posibles:

- `PROCESSING` — registro insertado al inicio, antes de ejecutar la compra.
- `COMPLETED` — compra exitosa; la respuesta JSON se serializa y guarda.
- `FAILED` — error de negocio (stock insuficiente, producto no encontrado,
  servicio caído); el error JSON se guarda.

Si llega un segundo request con la misma llave:

- `COMPLETED` → se devuelve exactamente la misma respuesta sin tocar inventario.
- `FAILED` → se relanza el error original.
- `PROCESSING` → 409 (compra en curso, posible request duplicado en vuelo).

**Por qué en PostgreSQL y no en Redis:** Redis añade una dependencia
infraestructural extra. Como los registros de idempotencia conviven en el mismo
motor que el inventario, la inserción del registro `PROCESSING` y el descuento
de stock pueden coordinarse en la misma unidad de trabajo, sin necesidad de
transacciones distribuidas.

**Race condition al insertar PROCESSING:** si dos requests llegan simultáneamente
con la misma llave antes de que ninguno haya insertado el registro, la constraint
`UNIQUE (idempotency_key)` garantiza que solo uno tendrá éxito. El otro recibe
`DataIntegrityViolationException`, que se convierte en 409.

**TTL y limpieza:** los registros expiran tras 24 horas (configurable via
`IDEMPOTENCY_TTL_HOURS`). Un `@Scheduled` cada hora borra los expirados para
mantener la tabla acotada.

---

## Resiliencia: Resilience4j con WebClient

La comunicación de `inventory-service` hacia `products-service` usa `WebClient`
(Reactor Netty) con timeouts explícitos en el cliente HTTP:

- **Connect timeout:** 2 s (si el socket no conecta en 2 s, falla rápido).
- **Response timeout:** 3 s (si products-service no responde en 3 s, falla).

Sobre eso se aplica Resilience4j:

| Mecanismo           | Configuración                                 | Propósito                                      |
|---------------------|-----------------------------------------------|------------------------------------------------|
| **Retry**           | 2 intentos, 200 ms entre ellos                | Absorber fallos transitorios de red            |
| **Circuit Breaker** | Ventana 10 llamadas, umbral 50%, 15 s abierto | Dejar de golpear un servicio caído             |
| **TimeLimiter**     | 3 s                                           | Cortar llamadas lentas antes de agotar threads |

**Distinción de errores:** el cliente distingue dos familias de fallo:

- `404` de products-service → `ProductNotFoundException` → 404 al cliente.
- Cualquier otro error de red o HTTP → `ProductServiceUnavailableException` → 503 al cliente.

Solo `ProductServiceUnavailableException` activa reintentos y circuit breaker.
`ProductNotFoundException` se propaga directamente sin reintentar, porque
reintentar un 404 nunca va a cambiar el resultado.

---

## Seguridad: JWT compartido + API Key interna

El JWT lo emite `products-service` y lo valida también `inventory-service`
usando el mismo secreto (`JWT_SECRET`). Esto evita un servicio de identidad
dedicado, que sería sobredimensionado para esta prueba.

**Tradeoff:** si el secreto rota, ambos servicios deben reiniciarse
coordinadamente. En producción real se usaría un par de claves asimétricas
(RS256) y `inventory-service` solo necesitaría la clave pública.

El endpoint `GET /internal/v1/products/{id}` de `products-service` está
protegido por `X-API-Key` en lugar de JWT. El frontend nunca llama a esa
ruta; solo la usa `inventory-service` con una clave que nunca sale al navegador.

---

## Evento InventoryChanged

Cuando una compra se completa, `PurchaseServiceImpl` emite un log estructurado:

```
InventoryChanged: productId={} quantityDeducted={} remainingStock={} correlationId={}
```

Este log cumple el mínimo requerido. La extensión natural sería publicar el
evento a un outbox o a Kafka/RabbitMQ para que otros servicios reaccionen sin
acoplamiento directo, pero queda fuera del alcance de esta prueba.

---

## Correlation ID

Cada request recibe un UUID generado por el filtro `CorrelationIdFilter`
(o reutiliza el header `X-Correlation-Id` si ya viene del caller). El ID se
propaga via `MDC` a todos los logs del request y se incluye en las respuestas
de error JSON:API. Esto permite reconstruir el flujo completo de una petición
a través de ambos servicios consultando los logs por el mismo `correlationId`.