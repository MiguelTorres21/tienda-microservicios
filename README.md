# Tienda Microservicios

Aplicación full stack basada en microservicios para administración de productos y compras con inventario.

El proyecto está compuesto por:

- `products-service`: CRUD de productos, autenticación JWT y endpoint interno protegido con API Key.
- `inventory-service`: consulta de inventario, compras con idempotencia, manejo de concurrencia y resiliencia frente a
  fallos de `products-service`.
- `frontend`: SPA en Vue 3 para login, listado, detalle de producto y flujo de compra.
- `infra`: orquestación de bases de datos y servicios backend con Docker Compose.

---

## Arquitectura

| Componente          | Puerto | Responsabilidad                                                                 |
|---------------------|-------:|---------------------------------------------------------------------------------|
| `products-service`  | `8081` | Login JWT, CRUD de productos, búsqueda, filtros, ordenamiento, endpoint interno |
| `inventory-service` | `8082` | Consulta de stock, compras, idempotencia, validación contra `products-service`  |
| `products-db`       | `5432` | Base de datos exclusiva de `products-service`                                   |
| `inventory-db`      | `5433` | Base de datos exclusiva de `inventory-service`                                  |
| `frontend`          | `5173` | Interfaz de administración                                                      |

Cada servicio backend tiene su propia base de datos PostgreSQL. La comunicación entre servicios se realiza vía HTTP; no
hay acceso cruzado a tablas entre microservicios.

---

## Stack técnico

### Backend

- Java 21
- Spring Boot 3.3.x
- Spring Web / WebFlux
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Resilience4j
- Spring Retry
- MapStruct
- Lombok
- OpenAPI / Swagger
- JUnit 5 / MockMvc / Testcontainers / JaCoCo

### Frontend

- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Axios
- Vitest
- Playwright

---

## Funcionalidades implementadas

### `products-service`

- Login con credenciales demo `admin / admin123`
- Emisión de JWT para el frontend
- CRUD completo de productos
- Paginación (`page`, `size`)
- Filtro por `status`
- Búsqueda por `sku` o `name`
- Ordenamiento por `price` o `createdAt`
- Endpoint interno protegido con `X-API-Key`
- Errores en formato JSON:API
- Correlation ID por request
- Health checks y Swagger

### `inventory-service`

- Consulta de inventario por `productId`
- Compra con JWT obligatorio
- Header `Idempotency-Key` obligatorio en `POST /api/v1/purchases`
- Reutilización segura de respuestas para requests repetidos con la misma key
- Manejo de stock insuficiente
- Manejo de producto inexistente
- Manejo de indisponibilidad de `products-service`
- Protección de concurrencia con optimistic locking + retry
- Logs estructurados con correlation ID
- Health checks y Swagger

### `frontend`

- Login
- Protección de rutas
- Listado de productos
- Filtros, búsqueda, ordenamiento y paginación
- Crear, editar y eliminar productos
- Ver detalle de producto
- Consultar inventario
- Ejecutar compras
- Manejo de errores de validación y negocio

---

## Estructura del proyecto

```text
.
├── docs/
│   └── technical-decisions.md
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── router/
│   │   ├── stores/
│   │   ├── types/
│   │   ├── views/
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   ├── vitest.config.ts
│   └── playwright.config.ts
├── infra/
│   └── docker-compose.yml
├── inventory-service/
│   ├── src/main/java/com/tienda/inventory/
│   ├── src/main/resources/
│   └── pom.xml
├── products-service/
│   ├── src/main/java/com/tienda/products/
│   ├── src/main/resources/
│   └── pom.xml
├── .env.example
└── README.md
```

---

## Requisitos

- Java 21
- Maven 3.9+
- Node.js 20+
- npm 10+
- Docker Desktop

---

## Variables de entorno

Copia el archivo de ejemplo:

```bash
cp .env.example .env
```

Variables principales:

| Variable                 | Descripción                                  | Valor por defecto                                 |
|--------------------------|----------------------------------------------|---------------------------------------------------|
| `SPRING_PROFILE`         | Perfil activo para backend                   | `dev`                                             |
| `JWT_SECRET`             | Secreto compartido para firmar/validar JWT   | `cambiar-este-secreto-en-produccion-min-32-chars` |
| `JWT_EXPIRATION_MS`      | Expiración del token                         | `3600000`                                         |
| `INTERNAL_API_KEY`       | API Key para comunicación interna            | `dev-internal-key-cambiar-en-produccion`          |
| `PRODUCTS_DB_PORT`       | Puerto PostgreSQL products                   | `5432`                                            |
| `INVENTORY_DB_PORT`      | Puerto PostgreSQL inventory                  | `5433`                                            |
| `PRODUCTS_SERVICE_PORT`  | Puerto products-service                      | `8081`                                            |
| `INVENTORY_SERVICE_PORT` | Puerto inventory-service                     | `8082`                                            |
| `RATE_LIMIT_CAPACITY`    | Capacidad del rate limit de products-service | `50`                                              |
| `RATE_LIMIT_REFILL`      | Refill por minuto                            | `50`                                              |
| `IDEMPOTENCY_TTL_HOURS`  | TTL de registros de idempotencia             | `24`                                              |

---

## Ejecución local

### 1. Levantar infraestructura

Desde `infra/`:

```bash
cd infra
docker compose up -d products-db inventory-db
```

### 2. Levantar `products-service`

```bash
cd products-service
mvn spring-boot:run
```

Servicio disponible en:

- `http://localhost:8081`
- Swagger: `http://localhost:8081/swagger-ui/index.html`
- Health: `http://localhost:8081/actuator/health`

### 3. Levantar `inventory-service`

```bash
cd inventory-service
mvn spring-boot:run
```

Servicio disponible en:

- `http://localhost:8082`
- Swagger: `http://localhost:8082/swagger-ui/index.html`
- Health: `http://localhost:8082/actuator/health`

### 4. Levantar frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend disponible en:

- `http://localhost:5173`

> El frontend usa proxy de Vite para redirigir:
> - `/api/v1/auth` y `/api/v1/products` hacia `http://localhost:8081`
> - `/api/v1/inventory` y `/api/v1/purchases` hacia `http://localhost:8082`

---

## Credenciales demo

```text
usuario: admin
contraseña: admin123
```

---

## Endpoints principales

### Auth

- `POST /api/v1/auth/login`

### Products

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `DELETE /api/v1/products/{id}`

### Internal products

- `GET /internal/v1/products/{id}`

### Inventory

- `GET /api/v1/inventory/{productId}`

### Purchases

- `POST /api/v1/purchases`

---

## Ejemplos rápidos

### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### Listar productos

```bash
curl "http://localhost:8081/api/v1/products?page=0&size=10&sortBy=createdAt&sortDir=desc" \
  -H "Authorization: Bearer TU_TOKEN"
```

### Consultar inventario

```bash
curl http://localhost:8082/api/v1/inventory/PRODUCT_ID \
  -H "Authorization: Bearer TU_TOKEN"
```

### Comprar

```bash
curl -X POST http://localhost:8082/api/v1/purchases \
  -H "Authorization: Bearer TU_TOKEN" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PRODUCT_ID",
    "quantity": 1
  }'
```

---

## Tests

### Frontend

```bash
cd frontend
npm run build
npm run test:unit
npm run test:e2e
```

### `products-service`

```bash
cd products-service
mvn test
```

### `inventory-service`

```bash
cd inventory-service
mvn test
```

---

## Cobertura

### `products-service`

- **Instructions:** 90%
- **Branches:** 62%

### `inventory-service`

- **Instructions:** 80%
- **Branches:** 73%

Los reportes JaCoCo se generan en:

```text
products-service/target/site/jacoco/index.html
inventory-service/target/site/jacoco/index.html
```

---

## Decisiones técnicas destacadas

Resumen:

- base de datos separada por microservicio
- optimistic locking + retry para concurrencia en compras
- idempotencia persistida con respuesta serializada
- Resilience4j para timeout, retry y circuit breaker
- JWT para frontend
- API Key para ruta interna de `products-service`
- correlation ID en logs y respuestas de error
- Flyway para migraciones y seed de datos en `dev`

Detalle completo en:

```text
docs/technical-decisions.md
```

---

## Notas

- El frontend guarda el JWT en `sessionStorage`.
- `inventory-service` valida el token con el mismo secreto compartido por `products-service`.
- Para ejecutar tests de integración con Testcontainers, Docker debe estar encendido.
- El archivo `README.md` original del proyecto estaba desactualizado respecto al estado final; este contenido
  corresponde al estado actual del repositorio.

---

## Posibles mejoras futuras

- refresh token / logout server-side
- observabilidad centralizada con trazas
- despliegue completo del frontend en contenedor dentro de `docker-compose`
- pipeline CI/CD
- contract testing entre servicios
- hardening adicional de seguridad para producción
