package com.example.vpoptimizer.mapper;

import com.example.vpoptimizer.entity.OptimizationSessionProduct;
import com.example.vpoptimizer.entity.Product;
import com.example.vpoptimizer.optimization.model.ProductOption;
import com.example.vpoptimizer.optimization.model.SelectionType;
import org.springframework.stereotype.Component;

/**
 * Translates persisted records into the engine's value object.
 *
 * <p>This is the only bridge between the persistence model and the optimization engine: the engine
 * itself never sees an entity.</p>
 */
@Component
public class ProductOptionMapper {

    public ProductOption toOption(Product product, boolean required) {
        return new ProductOption(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getMrp(),
                product.getVolumePoint(),
                product.getMinQuantity(),
                product.getMaxQuantity(),
                required ? SelectionType.REQUIRED : SelectionType.ALLOWED,
                null);
    }

    /**
     * Rebuilds an option from a persisted session snapshot, so reopening history shows exactly the
     * products and values that were used at the time.
     */
    public ProductOption toOption(OptimizationSessionProduct selection) {
        Long productId = selection.getProductId();
        if (productId == null) {
            // The catalogue row disappeared (ON DELETE SET NULL). Sessions still have to be readable,
            // so fall back to the (negative) snapshot row id - it stays unique and deterministic.
            productId = selection.getId() == null ? -1L : -selection.getId();
        }
        return new ProductOption(
                productId,
                selection.getProductName(),
                selection.getSku(),
                selection.getCategoryName(),
                selection.getMrp(),
                selection.getVolumePoint(),
                selection.getMinQuantity(),
                selection.getMaxQuantity(),
                selection.getSelectionType(),
                null);
    }
}
