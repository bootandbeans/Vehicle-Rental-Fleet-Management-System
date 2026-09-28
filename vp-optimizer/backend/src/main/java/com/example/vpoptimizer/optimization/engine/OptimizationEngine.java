package com.example.vpoptimizer.optimization.engine;

import com.example.vpoptimizer.optimization.model.OptimizationRequest;
import com.example.vpoptimizer.optimization.model.OptimizationResult;

/**
 * The optimization contract.
 *
 * <p>Implementations receive the products the user explicitly selected (plus optional
 * {@code REQUIRED} products) and return ranked purchase combinations. Implementations must be
 * free of Spring, JPA and HTTP concerns so that they can be swapped or improved independently -
 * see {@link IntegerOptimizationEngine} for the default dynamic-programming implementation.</p>
 */
public interface OptimizationEngine {

    /**
     * @param request a fully validated request (see {@link OptimizationRequest})
     * @return ranked solutions, closest alternatives and diagnostics; never {@code null}
     */
    OptimizationResult optimize(OptimizationRequest request);
}
