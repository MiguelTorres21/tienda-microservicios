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
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "createdAt");

    private final ProductRepository repository;
    private final ProductMapper mapper;

    /**
     * @param repository
     * @param mapper
     */
    public ProductServiceImpl(ProductRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        log.debug("Creando producto con SKU: {}", request.sku());

        if (repository.existsBySku(request.sku())) {
            throw new SkuAlreadyExistsException(request.sku());
        }

        Product saved = repository.save(mapper.toEntity(request));
        log.info("Producto creado: id={} sku={}", saved.getId(), saved.getSku());
        return mapper.toResponse(saved);
    }

    /**
     * @param id
     * @return
     */
    @Override
    public ProductResponse findById(UUID id) {
        log.debug("Buscando producto por id: {}", id);
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * @param sku
     * @return
     */
    @Override
    public ProductResponse findBySku(String sku) {
        log.debug("Buscando producto por sku: {}", sku);
        return repository.findBySku(sku)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(sku));
    }

    /**
     * @param status
     * @param search
     * @param sortBy
     * @param sortDir
     * @param page
     * @param size
     * @return
     */
    @Override
    public ProductPageResponse findAll(
            ProductStatus status,
            String search,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        String resolvedSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new InvalidSortFieldException(resolvedSortBy);
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));

        Specification<Product> spec = buildSpecification(status, search);

        log.debug("Listando productos: status={} search={} sortBy={} dir={} page={} size={}",
                status, search, resolvedSortBy, direction, page, size);

        Page<Product> resultPage = repository.findAll(spec, pageable);

        List<ProductResponse> data = resultPage.getContent()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return new ProductPageResponse(
                data,
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.getNumber(),
                resultPage.getSize()
        );
    }

    /**
     * @param id
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        log.debug("Actualizando producto id: {}", id);

        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (request.sku() != null && !request.sku().equals(product.getSku())
                && repository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new SkuAlreadyExistsException(request.sku());
        }

        mapper.updateEntityFromRequest(request, product);
        Product saved = repository.save(product);
        log.info("Producto actualizado: id={} sku={}", saved.getId(), saved.getSku());
        return mapper.toResponse(saved);
    }

    /**
     * @param id
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        log.debug("Eliminando producto id: {}", id);
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Producto eliminado: id={}", id);
    }

    /**
     * @param status
     * @param search
     * @return
     */
    private Specification<Product> buildSpecification(ProductStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            String trimmedSearch = (search != null) ? search.trim() : null;
            if (trimmedSearch != null && !trimmedSearch.isBlank()) {
                String pattern = "%" + trimmedSearch.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("sku")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
