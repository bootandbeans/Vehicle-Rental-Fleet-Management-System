package com.example.vpoptimizer.validation;

import com.example.vpoptimizer.dto.OptimizationRunRequest;
import com.example.vpoptimizer.optimization.model.ToleranceType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Bean validation of the optimization request (single fields and cross-field rules). */
class OptimizationRequestValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static OptimizationRunRequest request(List<Long> productIds, List<Long> requiredIds,
                                                 String discount, String gst, int targetVp,
                                                 ToleranceType type, String tolerance, Integer limit) {
        return new OptimizationRunRequest(productIds, requiredIds, new BigDecimal(discount), new BigDecimal(gst),
                targetVp, type, new BigDecimal(tolerance), limit, null);
    }

    @Test
    @DisplayName("a valid request passes validation")
    void acceptsValidRequest() {
        assertThat(validator.validate(request(List.of(1L, 2L), List.of(1L), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3))).isEmpty();
    }

    @Test
    @DisplayName("a required product must also be selected")
    void rejectsRequiredProductOutsideSelection() {
        var violations = validator.validate(request(List.of(1L), List.of(2L), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3));

        assertThat(violations).isNotEmpty();
        assertThat(violations).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).contains("requiredProductIds"));
    }

    @Test
    @DisplayName("negative discount, negative GST and negative target are rejected")
    void rejectsNegativeValues() {
        assertThat(validator.validate(request(List.of(1L), List.of(), "-1", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3))).isNotEmpty();
        assertThat(validator.validate(request(List.of(1L), List.of(), "20", "-1", 500,
                ToleranceType.PERCENTAGE, "10", 3))).isNotEmpty();
        assertThat(validator.validate(request(List.of(1L), List.of(), "20", "18", -5,
                ToleranceType.PERCENTAGE, "10", 3))).isNotEmpty();
        assertThat(validator.validate(request(List.of(1L), List.of(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "-10", 3))).isNotEmpty();
    }

    @Test
    @DisplayName("a discount above 100% is rejected, 0% GST and 0% discount are fine")
    void validatesRates() {
        assertThat(validator.validate(request(List.of(1L), List.of(), "120", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3))).isNotEmpty();
        assertThat(validator.validate(request(List.of(1L), List.of(), "0", "0", 500,
                ToleranceType.PERCENTAGE, "10", 3))).isEmpty();
    }

    @Test
    @DisplayName("a percentage tolerance above 100% is rejected")
    void rejectsLargePercentageTolerance() {
        var violations = validator.validate(request(List.of(1L), List.of(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "150", 3));

        assertThat(violations).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).contains("toleranceValue"));
    }

    @Test
    @DisplayName("duplicate product ids are rejected")
    void rejectsDuplicateProducts() {
        var violations = validator.validate(request(List.of(1L, 1L), List.of(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3));

        assertThat(violations).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).contains("productIds"));
    }

    @Test
    @DisplayName("a result limit below 1 is rejected")
    void rejectsInvalidResultLimit() {
        assertThat(validator.validate(request(List.of(1L), List.of(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 0))).isNotEmpty();
    }
}
