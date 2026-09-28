package com.example.vpoptimizer.service;

import com.example.vpoptimizer.dto.CategoryRequest;
import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.repository.CategoryRepository;
import com.example.vpoptimizer.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Category use cases and their guards. */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("a category still used by products cannot be deleted")
    void refusesDeletingCategoryInUse() {
        com.example.vpoptimizer.entity.Category category =
                new com.example.vpoptimizer.entity.Category("Nutrition", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.CATEGORY_IN_USE);
        verify(categoryRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("an unused category can be deleted")
    void deletesUnusedCategory() {
        com.example.vpoptimizer.entity.Category category =
                new com.example.vpoptimizer.entity.Category("Empty", null);
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(anyLong())).thenReturn(false);

        categoryService.delete(5L);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("duplicate category names are rejected")
    void rejectsDuplicateNames() {
        when(categoryRepository.existsByNameIgnoreCase("Nutrition")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Nutrition", null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("an unknown category is reported as not found")
    void rejectsUnknownCategory() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(42L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("categories are returned ordered by name with their product counts")
    void listsCategories() {
        com.example.vpoptimizer.entity.Category nutrition =
                mock(com.example.vpoptimizer.entity.Category.class);
        when(nutrition.getId()).thenReturn(1L);
        when(nutrition.getName()).thenReturn("Nutrition");
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(java.util.List.of(nutrition));
        when(productRepository.countProductsByCategory()).thenReturn(java.util.List.of(new Object[]{1L, 4L}));

        var categories = categoryService.findAll();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).name()).isEqualTo("Nutrition");
        assertThat(categories.get(0).productCount()).isEqualTo(4L);
    }
}
