package com.example.vpoptimizer.validation;

import com.example.vpoptimizer.dto.ProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates the min/max quantity pair of a {@link ProductRequest}. */
public class QuantityRangeValidator implements ConstraintValidator<ValidQuantityRange, ProductRequest> {

    @Override
    public boolean isValid(ProductRequest request, ConstraintValidatorContext context) {
        if (request == null || request.minQuantity() == null || request.maxQuantity() == null) {
            return true;
        }
        return request.maxQuantity() >= request.minQuantity();
    }
}
