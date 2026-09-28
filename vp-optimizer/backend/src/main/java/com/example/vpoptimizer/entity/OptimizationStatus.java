package com.example.vpoptimizer.entity;

/** Persisted outcome of an optimization session. */
public enum OptimizationStatus {

    /** At least one combination inside the accepted VP window was found. */
    COMPLETED,

    /** No combination reached the accepted VP window; only alternatives exist. */
    NO_VALID_SOLUTION
}
