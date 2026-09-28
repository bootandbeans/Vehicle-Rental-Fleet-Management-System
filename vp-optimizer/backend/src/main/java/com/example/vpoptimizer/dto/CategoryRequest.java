package com.example.vpoptimizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create / update payload for a category. */
public record CategoryRequest(

        @NotBlank(message = "Category name is required.")
        @Size(max = 80, message = "Category name must not exceed 80 characters.")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters.")
        String description) {
}
