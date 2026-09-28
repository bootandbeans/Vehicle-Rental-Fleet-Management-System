package com.example.vpoptimizer.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Product representation returned by the API. */
public record ProductResponse(Long id,
                              String name,
                              String sku,
                              String description,
                              BigDecimal mrp,
                              int volumePoint,
                              Long categoryId,
                              String categoryName,
                              Integer minQuantity,
                              Integer maxQuantity,
                              boolean active,
                              Instant createdAt,
                              Instant updatedAt) {
}
