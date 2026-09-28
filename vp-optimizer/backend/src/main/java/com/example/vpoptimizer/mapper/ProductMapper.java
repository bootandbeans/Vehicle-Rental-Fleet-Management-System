package com.example.vpoptimizer.mapper;

import com.example.vpoptimizer.dto.ProductRequest;
import com.example.vpoptimizer.dto.ProductResponse;
import com.example.vpoptimizer.entity.Category;
import com.example.vpoptimizer.entity.Product;

/** Maps between {@link Product} entities, API payloads and responses. */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getMrp(),
                product.getVolumePoint(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                product.getMinQuantity(),
                product.getMaxQuantity(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    /** Applies validated input to an entity (also used for updates, so it stays side-effect free). */
    public static void apply(ProductRequest request, Product product, Category category) {
        product.setName(request.name().trim());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setMrp(request.mrp());
        product.setVolumePoint(request.volumePoint());
        product.setCategory(category);
        product.setMinQuantity(request.minQuantity());
        product.setMaxQuantity(request.maxQuantity());
        if (request.active() != null) {
            product.setActive(request.active());
        }
    }
}
