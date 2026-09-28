package com.example.vpoptimizer.validation;

import com.example.vpoptimizer.dto.OptimizationRunRequest;
import com.example.vpoptimizer.optimization.model.ToleranceType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/** Implements {@link ValidOptimizationRequest}. */
public class OptimizationRequestValidator
        implements ConstraintValidator<ValidOptimizationRequest, OptimizationRunRequest> {

    private static final BigDecimal MAX_PERCENT_TOLERANCE = new BigDecimal("100");

    @Override
    public boolean isValid(OptimizationRunRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean valid = true;
        context.disableDefaultConstraintViolation();

        List<Long> productIds = request.productIds() == null ? List.of() : request.productIds();
        HashSet<Long> selected = new HashSet<>(productIds);

        if (productIds.stream().distinct().count() != productIds.size()) {
            valid = false;
            context.buildConstraintViolationWithTemplate("The same product was selected more than once.")
                    .addPropertyNode("productIds").addConstraintViolation();
        }

        for (Long requiredId : request.requiredProductIds()) {
            if (!selected.contains(requiredId)) {
                valid = false;
                context.buildConstraintViolationWithTemplate(
                                "A product can only be marked as required when it is selected too.")
                        .addPropertyNode("requiredProductIds").addConstraintViolation();
                break;
            }
        }

        if (request.toleranceType() == ToleranceType.PERCENTAGE
                && request.toleranceValue() != null
                && request.toleranceValue().compareTo(MAX_PERCENT_TOLERANCE) > 0) {
            valid = false;
            context.buildConstraintViolationWithTemplate("A percentage tolerance must not exceed 100 percent.")
                    .addPropertyNode("toleranceValue").addConstraintViolation();
        }

        return valid;
    }
}
