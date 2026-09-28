package com.example.vpoptimizer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cross-field validation of an optimization request: every required product must also be selected
 * and a percentage tolerance may not exceed 100%.
 */
@Documented
@Constraint(validatedBy = OptimizationRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOptimizationRequest {

    String message() default "The product selection is inconsistent.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
