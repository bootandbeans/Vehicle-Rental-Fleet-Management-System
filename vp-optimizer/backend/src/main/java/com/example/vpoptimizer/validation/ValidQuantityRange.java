package com.example.vpoptimizer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ensures {@code maxQuantity} is not smaller than {@code minQuantity}. */
@Documented
@Constraint(validatedBy = QuantityRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidQuantityRange {

    String message() default "Maximum quantity must not be smaller than the minimum quantity.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
