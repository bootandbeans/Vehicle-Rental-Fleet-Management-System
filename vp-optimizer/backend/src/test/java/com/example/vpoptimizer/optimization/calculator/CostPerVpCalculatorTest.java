package com.example.vpoptimizer.optimization.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Cost per VP must never divide by zero. */
class CostPerVpCalculatorTest {

    @Test
    @DisplayName("cost per VP is the payable amount divided by the VP total, scale 4")
    void calculatesCostPerVp() {
        assertThat(CostPerVpCalculator.calculate(new BigDecimal("19824.00"), 500))
                .contains(new BigDecimal("39.6480"));
    }

    @Test
    @DisplayName("zero VP yields an empty result instead of an exception")
    void handlesZeroVp() {
        assertThat(CostPerVpCalculator.calculate(new BigDecimal("500.00"), 0)).isEmpty();
        assertThat(CostPerVpCalculator.calculate(BigDecimal.ZERO, 0)).isEmpty();
    }

    @Test
    @DisplayName("negative VP is treated as no ratio")
    void handlesNegativeVp() {
        assertThat(CostPerVpCalculator.calculate(new BigDecimal("500.00"), -5)).isEmpty();
    }

    @Test
    @DisplayName("zero cost with positive VP is zero per VP")
    void handlesZeroCost() {
        assertThat(CostPerVpCalculator.calculate(BigDecimal.ZERO, 50)).contains(new BigDecimal("0.0000"));
    }
}
