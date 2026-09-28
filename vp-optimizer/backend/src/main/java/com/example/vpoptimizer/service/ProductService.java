package com.example.vpoptimizer.service;

import com.example.vpoptimizer.dto.PageResponse;
import com.example.vpoptimizer.dto.ProductQuery;
import com.example.vpoptimizer.dto.ProductRequest;
import com.example.vpoptimizer.dto.ProductResponse;
import com.example.vpoptimizer.entity.Category;
import com.example.vpoptimizer.entity.Product;
import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.exception.ResourceNotFoundException;
import com.example.vpoptimizer.mapper.ProductMapper;
import com.example.vpoptimizer.repository.OptimizationSessionProductRepository;
import com.example.vpoptimizer.repository.ProductRepository;
import com.example.vpoptimizer.repository.ProductSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Use cases for the persistent product catalogue. */
@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    /** Whitelisted API sort fields mapped to entity paths - no raw user input reaches the query. */
    private static final Map<String, String> SORTABLE_PROPERTIES = Map.ofEntries(
            Map.entry("name", "name"),
            Map.entry("sku", "sku"),
            Map.entry("mrp", "mrp"),
            Map.entry("volumePoint", "volumePoint"),
            Map.entry("active", "active"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("updatedAt", "updatedAt"),
            Map.entry("category", "category.name"));

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final OptimizationSessionProductRepository sessionProductRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryService categoryService,
                          OptimizationSessionProductRepository sessionProductRepository) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.sessionProductRepository = sessionProductRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductQuery query) {
        Sort sort = Sort.by(direction(query.direction()), property(query.sortBy()));
        Page<Product> page = productRepository.findAll(
                ProductSpecifications.build(query.search(), query.categoryId(), query.active()),
                PageRequest.of(query.page(), query.size(), sort));
        return PageResponse.of(page, ProductMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductMapper.toResponse(requireProduct(id));
    }

    public ProductResponse create(ProductRequest request) {
        validateSkuUniqueness(request.sku(), null);
        Product product = new Product(request.name().trim(), request.mrp(), request.volumePoint());
        Category category = request.categoryId() == null ? null : categoryService.requireCategory(request.categoryId());
        ProductMapper.apply(request, product, category);
        Product saved = productRepository.save(product);
        log.info("Created product {} ({})", saved.getId(), saved.getName());
        return ProductMapper.toResponse(saved);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = requireProduct(id);
        validateSkuUniqueness(request.sku(), id);
        Category category = request.categoryId() == null ? null : categoryService.requireCategory(request.categoryId());
        ProductMapper.apply(request, product, category);
        log.info("Updated product {}", id);
        return ProductMapper.toResponse(product);
    }

    public ProductResponse setActive(Long id, boolean active) {
        Product product = requireProduct(id);
        product.setActive(active);
        log.info("Product {} {}", id, active ? "activated" : "deactivated");
        return ProductMapper.toResponse(product);
    }

    /**
     * Deletes a product.
     *
     * <p>Soft deletion (the default) only flips {@code active}, so history keeps working. A hard
     * delete is allowed only when no optimization session references the product.</p>
     *
     * @param permanent when {@code true} the row is removed if it is safe to do so
     */
    public void delete(Long id, boolean permanent) {
        Product product = requireProduct(id);
        if (!permanent) {
            product.setActive(false);
            log.info("Soft deleted (deactivated) product {}", id);
            return;
        }
        if (sessionProductRepository.existsByProductId(id)) {
            throw new BusinessException(ApiErrorCode.PRODUCT_IN_USE,
                    "Product '" + product.getName() + "' is referenced by optimization history. "
                            + "It can be deactivated but not deleted.");
        }
        productRepository.delete(product);
        log.info("Permanently deleted product {}", id);
    }

    /**
     * Loads the products a user explicitly selected for an optimization.
     *
     * <p>Only the requested ids are queried - the rest of the catalogue is never loaded - and the
     * method fails loudly when a product is unknown or inactive, so the optimizer can never silently
     * use something the user does not expect.</p>
     */
    @Transactional(readOnly = true)
    public List<Product> requireSelectableProducts(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ApiErrorCode.NO_PRODUCTS_SELECTED);
        }
        List<Long> requested = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (requested.isEmpty()) {
            throw new BusinessException(ApiErrorCode.NO_PRODUCTS_SELECTED);
        }
        Map<Long, Product> found = new LinkedHashMap<>();
        for (Product product : productRepository.findAllByIdIn(requested)) {
            found.put(product.getId(), product);
        }
        List<Long> missing = requested.stream().filter(id -> !found.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND,
                    "These products do not exist: " + missing);
        }
        Set<Long> inactive = found.values().stream()
                .filter(product -> !product.isActive())
                .map(Product::getId)
                .collect(Collectors.toSet());
        if (!inactive.isEmpty()) {
            String names = found.values().stream()
                    .filter(product -> inactive.contains(product.getId()))
                    .map(Product::getName)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ApiErrorCode.PRODUCT_INACTIVE,
                    "Inactive products cannot participate in an optimization: " + names);
        }
        return requested.stream().map(found::get).toList();
    }

    /** Loads a product or fails with {@code PRODUCT_NOT_FOUND}. */
    @Transactional(readOnly = true)
    public Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND,
                        "Product " + id + " does not exist."));
    }

    private void validateSkuUniqueness(String sku, Long currentProductId) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        boolean exists = currentProductId == null
                ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, currentProductId);
        if (exists) {
            throw new BusinessException(ApiErrorCode.PRODUCT_SKU_ALREADY_EXISTS,
                    "A product with SKU '" + sku + "' already exists.");
        }
    }

    private static String property(String sortBy) {

        String property = sortBy == null ? null : SORTABLE_PROPERTIES.get(sortBy);
        if (property == null) {
            if (sortBy != null) {
                log.debug("Ignoring unsupported sort field '{}'", sortBy);
            }
            return SORTABLE_PROPERTIES.get("name");
        }
        return property;
    }

    private static Sort.Direction direction(String direction) {
        return Optional.ofNullable(direction)
                .map(value -> "desc".equalsIgnoreCase(value) ? Sort.Direction.DESC : Sort.Direction.ASC)
                .orElse(Sort.Direction.ASC);
    }
}
