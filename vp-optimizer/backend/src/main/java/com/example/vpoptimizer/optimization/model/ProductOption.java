package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A product as the optimization engine sees it: a pure value object with no persistence concerns.
 *
 * @param id             product id (used for canonical keys and deterministic ordering)
 * @param name           display name
 * @param sku            optional SKU
 * @param categoryName   optional category label (used for reporting only)
 * @param mrp            maximum retail price, never negative
 * @param volumePoint    VP granted per unit, never negative
 * @param minQuantity    catalogue minimum quantity (nullable). Only enforced for {@link SelectionType#REQUIRED}
 * @param maxQuantity    catalogue maximum quantity (nullable means unbounded, capped by engine limits)
 * @param selectionType  whether the product is optional or mandatory
 * @param pricingOverride optional product specific pricing (nullable)
 */
public record ProductOption(Long id,
                            String name,
                            String sku,
                            String categoryName,
                            BigDecimal mrp,
                            int volumePoint,
                            Integer minQuantity,
                            Integer maxQuantity,
                            SelectionType selectionType,
                            PricingOverride pricingOverride) {

    public ProductOption {
        Objects.requireNonNull(id, "A product option needs an id.");
        Objects.requireNonNull(name, "A product option needs a name.");
        Objects.requireNonNull(mrp, "A product option needs an MRP.");
        Objects.requireNonNull(selectionType, "A product option needs a selection type.");
        if (mrp.signum() < 0) {
            throw new BusinessException(ApiErrorCode.VALIDATION_FAILED, "MRP must not be negative.");
        }
        if (volumePoint < 0) {
            throw new BusinessException(ApiErrorCode.VALIDATION_FAILED, "Volume points must not be negative.");
        }
        if (minQuantity != null && minQuantity < 0) {
            throw new BusinessException(ApiErrorCode.INVALID_QUANTITY_RANGE, "Minimum quantity must not be negative.");
        }
        if (maxQuantity != null && maxQuantity < 1) {
            throw new BusinessException(ApiErrorCode.INVALID_QUANTITY_RANGE, "Maximum quantity must be at least 1.");
        }
        if (minQuantity != null && maxQuantity != null && maxQuantity < minQuantity) {
            throw new BusinessException(ApiErrorCode.INVALID_QUANTITY_RANGE,
                    "Maximum quantity must not be smaller than the minimum quantity.");
        }
    }

    /** Convenience factory for an optional product with global pricing. */
    public static ProductOption allowed(Long id, String name, BigDecimal mrp, int volumePoint) {
        return new ProductOption(id, name, null, null, mrp, volumePoint, null, null, SelectionType.ALLOWED, null);
    }

    public boolean required() {
        return selectionType == SelectionType.REQUIRED;
    }

    public ProductOption withSelectionType(SelectionType type) {
        return new ProductOption(id, name, sku, categoryName, mrp, volumePoint, minQuantity, maxQuantity, type,
                pricingOverride);
    }
}
