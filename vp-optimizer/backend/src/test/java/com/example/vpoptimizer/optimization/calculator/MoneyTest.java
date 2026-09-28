package com.example.vpoptimizer.optimization.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rounding policy and money helpers. */
class MoneyTest {

    @Test
    @DisplayName("money is rounded to 2 decimals HALF_UP")
    void roundsMoneyHalfUp() {
        assertThat(Money.scale(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(Money.scale(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
        assertThat(Money.scale(new BigDecimal("2.5"))).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("rates are rounded to 4 decimals HALF_UP")
    void roundsRates() {
        assertThat(Money.scaleRate(new BigDecimal("18.12345"))).isEqualByComparingTo("18.1235");
    }

    @Test
    @DisplayName("percentOf multiplies and rounds")
    void computesPercentages() {
        assertThat(Money.percentOf(new BigDecimal("2000"), new BigDecimal("20"))).isEqualByComparingTo("400.00");
        assertThat(Money.percentOf(new BigDecimal("1600"), new BigDecimal("18"))).isEqualByComparingTo("288.00");
        assertThat(Money.percentOf(new BigDecimal("999.99"), new BigDecimal("7.5"))).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("minor units round trip exactly")
    void convertsToMinorUnits() {
        assertThat(Money.toMinorUnits(new BigDecimal("1888.00"))).isEqualTo(188_800L);
        assertThat(Money.fromMinorUnits(188_800L)).isEqualByComparingTo("1888.00");
        assertThat(Money.toMinorUnits(new BigDecimal("0.01"))).isEqualTo(1L);
    }

    @Test
    @DisplayName("subtle decimal values are rejected instead of silently truncated")
    void rejectsUnroundedAmounts() {
        assertThatThrownBy(() -> Money.toMinorUnits(new BigDecimal("10.005")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("formatting uses the rupee symbol and 2 decimals")
    void formatsAmounts() {
        assertThat(Money.format(new BigDecimal("19824"))).isEqualTo("₹19,824.00");
        assertThat(Money.format(null)).isEqualTo("-");
    }
}
