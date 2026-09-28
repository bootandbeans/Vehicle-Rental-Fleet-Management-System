package com.example.vpoptimizer.optimization.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Builds the permutation independent identity of a combination.
 *
 * <p>{@code A x 2 + B x 3} and {@code B x 3 + A x 2} are the same purchase, so both normalize to
 * the same key - here {@code 1:2|2:3} for product ids 1 and 2. Only products with a positive
 * quantity take part, so the key never depends on ordering or on zero-quantity entries.</p>
 */
public final class CanonicalKey {

    private CanonicalKey() {
    }

    /** Key for a list of product lines, e.g. {@code 1:2|2:3}. */
    public static String of(Collection<ProductQuantity> lines) {
        TreeMap<Long, Integer> quantities = new TreeMap<>();
        for (ProductQuantity line : lines) {
            if (line.quantity() > 0) {
                quantities.merge(line.productId(), line.quantity(), Integer::sum);
            }
        }
        return ofQuantities(quantities);
    }

    /** Key for an explicit id/quantity map. Quantities &lt;= 0 are ignored. */
    public static String ofQuantities(TreeMap<Long, Integer> quantities) {
        StringJoiner joiner = new StringJoiner("|");
        quantities.forEach((productId, quantity) -> {
            if (quantity != null && quantity > 0) {
                joiner.add(productId + ":" + quantity);
            }
        });
        return joiner.toString();
    }

    /** Stable ordering of product lines used by every consumer of a solution. */
    public static List<ProductQuantity> sortLines(List<ProductQuantity> lines) {
        return lines.stream()
                .sorted(Comparator.comparing(ProductQuantity::productId))
                .toList();
    }
}
