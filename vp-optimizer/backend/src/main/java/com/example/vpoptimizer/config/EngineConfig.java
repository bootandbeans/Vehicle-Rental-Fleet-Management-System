package com.example.vpoptimizer.config;

import com.example.vpoptimizer.optimization.calculator.PricingCalculator;
import com.example.vpoptimizer.optimization.calculator.StandardPricingCalculator;
import com.example.vpoptimizer.optimization.engine.IntegerOptimizationEngine;
import com.example.vpoptimizer.optimization.engine.OptimizationEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the pure-Java optimization components as Spring beans.
 *
 * <p>The engine and the pricing calculator know nothing about Spring: they are plain objects that
 * this configuration exposes as beans. Swapping in a different engine (branch and bound, an ILP
 * solver, ...) is a one-line change here.</p>
 */
@Configuration
@EnableConfigurationProperties({OptimizationProperties.class, CorsProperties.class})
public class EngineConfig {

    @Bean
    public PricingCalculator pricingCalculator() {
        return new StandardPricingCalculator();
    }

    @Bean
    public OptimizationEngine optimizationEngine(PricingCalculator pricingCalculator) {
        return new IntegerOptimizationEngine(pricingCalculator);
    }
}
