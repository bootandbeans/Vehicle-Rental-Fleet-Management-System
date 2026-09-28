package com.example.vpoptimizer.optimization.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Duplicate (permuted) combinations must collapse into one identity. */
class CanonicalKeyTest {

    private static ProductQuantity line(long id, int quantity) {
        return new ProductQuantity(id, "Product " + id, null, null, quantity, new BigDecimal("100"), 10,
                new BigDecimal("20"), new BigDecimal("18"), new BigDecimal("20.00"), new BigDecimal("80.00"),
                new BigDecimal("14.40"), new BigDecimal("94.40"), new BigDecimal("94.40").multiply(BigDecimal.valueOf(quantity)),
                10L * quantity, false);
    }

    @Test
    @DisplayName("A x 2 + B x 3 and B x 3 + A x 2 share the same key")
    void normalizesPermutations() {
        String first = CanonicalKey.of(List.of(line(1, 2), line(2, 3)));
        String second = CanonicalKey.of(List.of(line(2, 3), line(1, 2)));

        assertThat(first).isEqualTo("1:2|2:3");
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("quantities sum when the same product appears twice in the input")
    void mergesDuplicateLines() {
        assertThat(CanonicalKey.of(List.of(line(1, 2), line(1, 3)))).isEqualTo("1:5");
    }

    @Test
    @DisplayName("zero quantities are ignored")
    void ignoresEmptyLines() {
        assertThat(CanonicalKey.of(List.of(line(1, 2), line(9, 0)))).isEqualTo("1:2");
        assertThat(CanonicalKey.of(List.of(line(9, 0)))).isEmpty();
    }

    @Test
    @DisplayName("lines are sorted by product id for stable output")
    void sortsLines() {
        List<ProductQuantity> sorted = CanonicalKey.sortLines(List.of(line(7, 1), line(2, 1), line(5, 1)));

        assertThat(sorted).extracting(ProductQuantity::productId).containsExactly(2L, 5L, 7L);
    }
}
