package com.example.vpoptimizer.dto;

import jakarta.validation.constraints.NotNull;

/** Payload of {@code PATCH /api/products/{id}/active}. */
public record ProductActiveRequest(@NotNull(message = "The active flag is required.") Boolean active) {
}
