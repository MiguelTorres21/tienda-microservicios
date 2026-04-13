package com.tienda.products.service;

import com.tienda.products.domain.Product;
import com.tienda.products.domain.ProductStatus;
import com.tienda.products.dto.ProductPageResponse;
import com.tienda.products.dto.ProductRequest;
import com.tienda.products.dto.ProductResponse;
import com.tienda.products.exception.InvalidSortFieldException;
import com.tienda.products.exception.ProductNotFoundException;
import com.tienda.products.exception.SkuAlreadyExistsException;
import com.tienda.products.mapper.ProductMapper;
import com.tienda.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl")
class ProductServiceImplTest {

    @Mock
    ProductRepository repository;
    @Mock
    ProductMapper mapper;

    @InjectMocks
    ProductServiceImpl service;

    private UUID productId;
    private Product existingProduct;
    private ProductResponse existingResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        existingProduct = Product.builder()
                .id(productId)
                .sku("SKU-001")
                .name("Laptop Pro")
                .price(new BigDecimal("999.99"))
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        existingResponse = new ProductResponse(
                productId, "SKU-001", "Laptop Pro",
                new BigDecimal("999.99"), ProductStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path: guarda y devuelve el producto creado")
        void create_savesAndReturnsProduct() {
            ProductRequest request = new ProductRequest(
                    "SKU-NUEVO", "Nuevo Producto", new BigDecimal("50.00"), ProductStatus.ACTIVE);

            when(repository.existsBySku("SKU-NUEVO")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(existingProduct);
            when(repository.save(existingProduct)).thenReturn(existingProduct);
            when(mapper.toResponse(existingProduct)).thenReturn(existingResponse);

            ProductResponse result = service.create(request);

            assertThat(result).isNotNull();
            verify(repository).save(existingProduct);
        }

        @Test
        @DisplayName("lanza SkuAlreadyExistsException si el SKU ya existe")
        void create_throwsSkuAlreadyExists_whenSkuTaken() {
            ProductRequest request = new ProductRequest(
                    "SKU-001", "Otro", new BigDecimal("10.00"), ProductStatus.ACTIVE);

            when(repository.existsBySku("SKU-001")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(SkuAlreadyExistsException.class)
                    .hasMessageContaining("SKU-001");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("happy path: devuelve el producto cuando existe")
        void findById_returnsProduct_whenExists() {
            when(repository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(mapper.toResponse(existingProduct)).thenReturn(existingResponse);

            ProductResponse result = service.findById(productId);

            assertThat(result.id()).isEqualTo(productId);
        }

        @Test
        @DisplayName("lanza ProductNotFoundException cuando no existe")
        void findById_throwsNotFound_whenMissing() {
            when(repository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining(productId.toString());
        }
    }


    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("happy path: actualiza y devuelve el producto")
        void update_updatesAndReturnsProduct() {
            ProductRequest request = new ProductRequest(
                    "SKU-001", "Laptop Updated", new BigDecimal("1099.99"), ProductStatus.ACTIVE);

            when(repository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(repository.save(existingProduct)).thenReturn(existingProduct);
            when(mapper.toResponse(existingProduct)).thenReturn(existingResponse);

            ProductResponse result = service.update(productId, request);

            assertThat(result).isNotNull();
            verify(mapper).updateEntityFromRequest(request, existingProduct);
            verify(repository).save(existingProduct);
        }

        @Test
        @DisplayName("lanza ProductNotFoundException cuando el producto no existe")
        void update_throwsNotFound_whenProductMissing() {
            ProductRequest request = new ProductRequest(
                    "SKU-X", "X", new BigDecimal("10.00"), ProductStatus.ACTIVE);

            when(repository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(productId, request))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza SkuAlreadyExistsException si el nuevo SKU pertenece a otro producto")
        void update_throwsSkuConflict_whenSkuTakenByOther() {
            ProductRequest request = new ProductRequest(
                    "SKU-OTRO", "X", new BigDecimal("10.00"), ProductStatus.ACTIVE);

            when(repository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(repository.existsBySkuAndIdNot("SKU-OTRO", productId)).thenReturn(true);

            assertThatThrownBy(() -> service.update(productId, request))
                    .isInstanceOf(SkuAlreadyExistsException.class)
                    .hasMessageContaining("SKU-OTRO");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("permite actualizar el producto con el mismo SKU (no conflicto)")
        void update_allowsSameSku_noConflict() {
            ProductRequest request = new ProductRequest(
                    "SKU-001", "Laptop Renamed", new BigDecimal("999.99"), ProductStatus.ACTIVE);

            when(repository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(repository.save(existingProduct)).thenReturn(existingProduct);
            when(mapper.toResponse(existingProduct)).thenReturn(existingResponse);

            assertThat(service.update(productId, request)).isNotNull();
            verify(repository).save(existingProduct);
        }
    }


    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("happy path: elimina el producto cuando existe")
        void delete_removesProduct_whenExists() {
            when(repository.existsById(productId)).thenReturn(true);

            service.delete(productId);

            verify(repository).deleteById(productId);
        }

        @Test
        @DisplayName("lanza ProductNotFoundException cuando no existe")
        void delete_throwsNotFound_whenMissing() {
            when(repository.existsById(productId)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining(productId.toString());

            verify(repository, never()).deleteById(any());
        }
    }


    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("lanza InvalidSortFieldException para sortBy no permitido")
        void findAll_throwsInvalidSort_forUnknownSortField() {
            assertThatThrownBy(() ->
                    service.findAll(null, null, "unknownField", "asc", 0, 10))
                    .isInstanceOf(InvalidSortFieldException.class)
                    .hasMessageContaining("unknownField");
        }

        @Test
        @DisplayName("acepta sortBy=price sin lanzar excepción")
        void findAll_acceptsSortByPrice() {
            Page<Product> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            ProductPageResponse result = service.findAll(null, null, "price", "asc", 0, 10);

            assertThat(result.data()).isEmpty();
            assertThat(result.totalItems()).isEqualTo(0);
        }

        @Test
        @DisplayName("acepta sortBy=createdAt sin lanzar excepción")
        void findAll_acceptsSortByCreatedAt() {
            Page<Product> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            ProductPageResponse result = service.findAll(null, null, "createdAt", "desc", 0, 10);

            assertThat(result.data()).isEmpty();
        }

        @Test
        @DisplayName("mapea correctamente la página devuelta por el repositorio")
        void findAll_mapsPageCorrectly() {
            Product secondProduct = Product.builder()
                    .id(UUID.randomUUID())
                    .sku("SKU-002")
                    .name("Mouse")
                    .price(new BigDecimal("50.00"))
                    .status(ProductStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Product thirdProduct = Product.builder()
                    .id(UUID.randomUUID())
                    .sku("SKU-003")
                    .name("Keyboard")
                    .price(new BigDecimal("70.00"))
                    .status(ProductStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Page<Product> page = new PageImpl<>(
                    List.of(existingProduct, secondProduct, thirdProduct),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    3L
            );

            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);
            when(mapper.toResponse(any(Product.class))).thenReturn(existingResponse);

            ProductPageResponse result = service.findAll(null, null, "createdAt", "desc", 0, 10);

            assertThat(result.data()).hasSize(3);
            assertThat(result.totalItems()).isEqualTo(3L);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.currentPage()).isEqualTo(0);
            assertThat(result.pageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("usa sortDir desc por defecto para valores desconocidos")
        void findAll_defaultsToDesc_forUnknownSortDir() {
            Page<Product> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            service.findAll(null, null, "createdAt", "random", 0, 10);

            verify(repository).findAll(
                    any(Specification.class),
                    argThat((Pageable p) -> {
                        var order = p.getSort().getOrderFor("createdAt");
                        return order != null && order.isDescending();
                    })
            );
        }

        @Test
        @DisplayName("usa sortBy=createdAt cuando el campo viene nulo")
        void findAll_defaultsToCreatedAt_whenSortByNull() {
            Page<Product> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            service.findAll(null, null, null, "desc", 0, 10);

            verify(repository).findAll(
                    any(Specification.class),
                    argThat((Pageable p) -> p.getSort().getOrderFor("createdAt") != null)
            );
        }
    }
}
