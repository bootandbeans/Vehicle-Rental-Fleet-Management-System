package com.example.vpoptimizer.dto;

import java.time.Instant;

/** Category representation returned by the API. */
public record CategoryResponse(Long id,
                               String name,
                               String description,
                               long productCount,
                               Instant createdAt,
                               Instant updatedAt) {
}
