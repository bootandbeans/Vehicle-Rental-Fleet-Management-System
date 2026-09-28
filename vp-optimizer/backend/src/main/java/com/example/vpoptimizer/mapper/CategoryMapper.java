package com.example.vpoptimizer.mapper;

import com.example.vpoptimizer.dto.CategoryResponse;
import com.example.vpoptimizer.entity.Category;

/** Maps {@link Category} entities to API responses. */
public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category, long productCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                productCount,
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public static CategoryResponse toResponse(Category category) {
        return toResponse(category, 0L);
    }
}
