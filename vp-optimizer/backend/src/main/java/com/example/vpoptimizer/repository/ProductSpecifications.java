package com.example.vpoptimizer.repository;

import com.example.vpoptimizer.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Composable query predicates for the product catalogue. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Free text match on name or SKU. */
    public static Specification<Product> matchesText(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return builder.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("sku")), pattern));
        };
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, builder) -> categoryId == null
                ? builder.conjunction()
                : builder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasActive(Boolean active) {
        return (root, query, builder) -> active == null
                ? builder.conjunction()
                : builder.equal(root.get("active"), active);
    }

    /** Combines every filter, ignoring {@code null} inputs. */
    public static Specification<Product> build(String search, Long categoryId, Boolean active) {
        List<Specification<Product>> specifications = new ArrayList<>();
        specifications.add(matchesText(search));
        specifications.add(hasCategory(categoryId));
        specifications.add(hasActive(active));
        return specifications.stream()
                .reduce(Specification::and)
                .orElse((root, query, builder) -> builder.conjunction());
    }
}
