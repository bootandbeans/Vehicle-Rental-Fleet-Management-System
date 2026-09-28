package com.example.vpoptimizer.config;

import com.example.vpoptimizer.optimization.model.OptimizationLimits;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Engine safety limits, bound from {@code vp-optimizer.optimization.*}.
 *
 * <p>Keeping them in configuration (not in the algorithm) means a deployment can tune the
 * supported catalogue size and VP range without code changes.</p>
 */
@Validated
@ConfigurationProperties(prefix = "vp-optimizer.optimization")
public class OptimizationProperties {

    @Min(1)
    private int defaultResultLimit = 3;

    @Min(1)
    private int maximumResultLimit = 10;

    @Min(1)
    private int maxSelectedProducts = 25;

    @Min(1)
    private int defaultMaxQuantityPerProduct = 10;

    @Min(1)
    private int maxVpCapacity = 20_000;

    /** Immutable value object handed to the engine. */
    public OptimizationLimits toLimits() {
        return new OptimizationLimits(defaultResultLimit, maximumResultLimit, maxSelectedProducts,
                defaultMaxQuantityPerProduct, maxVpCapacity);
    }

    public int getDefaultResultLimit() {
        return defaultResultLimit;
    }

    public void setDefaultResultLimit(int defaultResultLimit) {
        this.defaultResultLimit = defaultResultLimit;
    }

    public int getMaximumResultLimit() {
        return maximumResultLimit;
    }

    public void setMaximumResultLimit(int maximumResultLimit) {
        this.maximumResultLimit = maximumResultLimit;
    }

    public int getMaxSelectedProducts() {
        return maxSelectedProducts;
    }

    public void setMaxSelectedProducts(int maxSelectedProducts) {
        this.maxSelectedProducts = maxSelectedProducts;
    }

    public int getDefaultMaxQuantityPerProduct() {
        return defaultMaxQuantityPerProduct;
    }

    public void setDefaultMaxQuantityPerProduct(int defaultMaxQuantityPerProduct) {
        this.defaultMaxQuantityPerProduct = defaultMaxQuantityPerProduct;
    }

    public int getMaxVpCapacity() {
        return maxVpCapacity;
    }

    public void setMaxVpCapacity(int maxVpCapacity) {
        this.maxVpCapacity = maxVpCapacity;
    }
}
