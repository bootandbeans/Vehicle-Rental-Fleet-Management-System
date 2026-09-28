package com.example.vpoptimizer.optimization.model;

/** How a product takes part in an optimization. */
public enum SelectionType {

    /** The optimizer may use the product (zero or more units). */
    ALLOWED,

    /** The optimizer must use the product at least {@code minimumQuantity} times (default 1). */
    REQUIRED
}
