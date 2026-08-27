package com.rental.model;

/**
 * Internal validation helpers shared by the domain model.
 *
 * <p>Package-private on purpose: validation rules belong to the model
 * package and are not part of the public API.
 */
final class Validators {

    private Validators() {
        // utility class
    }

    /**
     * Requires a non-null, non-blank value and returns it trimmed.
     *
     * @param value the value to check
     * @param field name of the field, used in the error message
     * @return the trimmed value
     * @throws IllegalArgumentException if the value is null or blank
     */
    static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
