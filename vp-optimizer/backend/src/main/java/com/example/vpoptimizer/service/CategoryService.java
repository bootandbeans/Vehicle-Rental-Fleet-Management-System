package com.example.vpoptimizer.service;

import com.example.vpoptimizer.dto.CategoryRequest;
import com.example.vpoptimizer.dto.CategoryResponse;
import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.exception.ResourceNotFoundException;
import com.example.vpoptimizer.entity.Category;
import com.example.vpoptimizer.mapper.CategoryMapper;
import com.example.vpoptimizer.repository.CategoryRepository;
import com.example.vpoptimizer.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CRUD use cases for product categories. */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        Map<Long, Long> counts = productCountsByCategory();
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> CategoryMapper.toResponse(category, counts.getOrDefault(category.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = requireCategory(id);
        return CategoryMapper.toResponse(category, productRepository.countByCategoryId(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ApiErrorCode.CATEGORY_NAME_ALREADY_EXISTS,
                    "A category named '" + name + "' already exists.");
        }
        Category category = new Category(name, trimToNull(request.description()));
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = requireCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ApiErrorCode.CATEGORY_NAME_ALREADY_EXISTS,
                    "A category named '" + name + "' already exists.");
        }
        category.setName(name);
        category.setDescription(trimToNull(request.description()));
        return CategoryMapper.toResponse(category, productRepository.countByCategoryId(id));
    }

    public void delete(Long id) {
        Category category = requireCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(ApiErrorCode.CATEGORY_IN_USE,
                    "Category '" + category.getName() + "' is still assigned to products. "
                            + "Reassign those products before deleting the category.");
        }
        categoryRepository.delete(category);
    }

    /** Loads a category or fails with {@code CATEGORY_NOT_FOUND}. */
    @Transactional(readOnly = true)
    public Category requireCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CATEGORY_NOT_FOUND,
                        "Category " + id + " does not exist."));
    }

    private Map<Long, Long> productCountsByCategory() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : productRepository.countProductsByCategory()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
