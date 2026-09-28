package com.example.vpoptimizer.optimization.calculator;

import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.PricingDetails;
import com.example.vpoptimizer.optimization.model.PricingOverride;
import com.example.vpoptimizer.optimization.model.ProductOption;
import com.example.vpoptimizer.optimization.model.SelectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pricing engine tests.
 *
 * <p>Covers the specification examples verbatim: 0% GST and 0% discount must work, rounding follows
 * the documented HALF_UP policy and product specific rates override the global ones.</p>
 */
class StandardPricingCalculatorTest {

    private final PricingCalculator calculator = new StandardPricingCalculator();

    private static ProductOption product(long id, String mrp) {
        return new ProductOption(id, "Product " + id, null, null, new BigDecimal(mrp), 10, null, null,
                SelectionType.ALLOWED, null);
    }

    @Test
    @DisplayName("MRP 1000, 20% discount, 18% GST -> 800 + 144 = 944")
    void pricesTheSpecificationExample() {
        PricingDetails pricing = calculator.priceUnit(product(1, "1000"), new BigDecimal("20"), new BigDecimal("18"));

        assertThat(pricing.discountAmount()).isEqualByComparingTo("200.00");
        assertThat(pricing.discountedPrice()).isEqualByComparingTo("800.00");
        assertThat(pricing.gstAmount()).isEqualByComparingTo("144.00");
        assertThat(pricing.finalPrice()).isEqualByComparingTo("944.00");
    }

    @Test
    @DisplayName("MRP 2000, 20% discount, 18% GST -> 1600 + 288 = 1888")
    void pricesTheSecondSpecificationExample() {
        PricingDetails pricing = calculator.priceUnit(product(1, "2000"), new BigDecimal("20"), new BigDecimal("18"));

        assertThat(pricing.discountedPrice()).isEqualByComparingTo("1600.00");
        assertThat(pricing.gstAmount()).isEqualByComparingTo("288.00");
        assertThat(pricing.finalPrice()).isEqualByComparingTo("1888.00");
    }

    @Test
    @DisplayName("0% GST is supported (final price equals the discounted price)")
    void supportsZeroGst() {
        PricingDetails pricing = calculator.priceUnit(product(1, "1000"), new BigDecimal("20"), BigDecimal.ZERO);

        assertThat(pricing.gstAmount()).isEqualByComparingTo("0.00");
        assertThat(pricing.finalPrice()).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("0% discount is supported (GST applies to the full MRP)")
    void supportsZeroDiscount() {
        PricingDetails pricing = calculator.priceUnit(product(1, "1000"), BigDecimal.ZERO, new BigDecimal("18"));

        assertThat(pricing.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(pricing.discountedPrice()).isEqualByComparingTo("1000.00");
        assertThat(pricing.finalPrice()).isEqualByComparingTo("1180.00");
    }

    @Test
    @DisplayName("both 0% discount and 0% GST leave the MRP unchanged")
    void supportsZeroDiscountAndZeroGst() {
        PricingDetails pricing = calculator.priceUnit(product(1, "1234.56"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(pricing.finalPrice()).isEqualByComparingTo("1234.56");
    }

    @Test
    @DisplayName("rounding is applied after every step with HALF_UP")
    void roundsEveryStep() {
        // 999.99 - 7.5% = 924.99075 -> 924.99 ; GST 18% of 924.99 = 166.4982 -> 166.50
        PricingDetails pricing = calculator.priceUnit(product(1, "999.99"), new BigDecimal("7.5"), new BigDecimal("18"));

        assertThat(pricing.discountAmount()).isEqualByComparingTo("75.00");
        assertThat(pricing.discountedPrice()).isEqualByComparingTo("924.99");
        assertThat(pricing.gstAmount()).isEqualByComparingTo("166.50");
        assertThat(pricing.finalPrice()).isEqualByComparingTo("1091.49");
    }

    @Test
    @DisplayName("a fractional discount rate is rounded to 4 decimals before use")
    void roundsFractionalRates() {
        PricingDetails pricing = calculator.priceUnit(product(1, "100"), new BigDecimal("33.33333"), BigDecimal.ZERO);

        assertThat(pricing.discountPercent()).isEqualByComparingTo("33.3333");
        assertThat(pricing.discountedPrice()).isEqualByComparingTo("66.67");
    }

    @Test
    @DisplayName("product specific rates override the global rates")
    void supportsProductSpecificRates() {
        ProductOption special = new ProductOption(7L, "Special", null, null, new BigDecimal("1000"), 10, null, null,
                SelectionType.ALLOWED, new PricingOverride(new BigDecimal("25"), BigDecimal.ZERO));
        PricingContext context = new PricingContext(new BigDecimal("20"), new BigDecimal("18"),
                Map.of(7L, special.pricingOverride()));

        PricingDetails pricing = calculator.priceUnit(special, context);

        assertThat(pricing.discountPercent()).isEqualByComparingTo("25");
        assertThat(pricing.gstPercent()).isEqualByComparingTo("0");
        assertThat(pricing.finalPrice()).isEqualByComparingTo("750.00");
    }

    @Test
    @DisplayName("without an override the global rates are used")
    void fallsBackToGlobalRates() {
        PricingContext context = PricingContext.global(new BigDecimal("20"), new BigDecimal("18"));

        PricingDetails pricing = calculator.priceUnit(product(1, "2000"), context);

        assertThat(pricing.finalPrice()).isEqualByComparingTo("1888.00");
    }

    @Test
    @DisplayName("line totals multiply the unit price by the quantity")
    void computesLineTotals() {
        PricingDetails pricing = calculator.priceUnit(product(1, "2000"), new BigDecimal("20"), new BigDecimal("18"));

        assertThat(calculator.lineTotal(pricing, 3)).isEqualByComparingTo("5664.00");
        assertThat(calculator.lineTotal(pricing, 0)).isEqualByComparingTo("0.00");
    }
}
