package com.rental.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A customer of the rental company.
 *
 * <p>Encapsulation: identity fields are validated at construction time;
 * {@link #setEmail(String)} demonstrates validated setter behaviour. The
 * reservation history is exposed only as an unmodifiable copy.
 */
public final class Customer {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String PHONE_PATTERN = "^\\d{7,15}$";

    private final String id;
    private final String name;
    private String email;
    private final String phone;
    private final List<Reservation> reservations = new ArrayList<>();

    /**
     * Creates a customer.
     *
     * @param id    unique customer id, e.g. {@code CUST-001}
     * @param name  non-blank full name
     * @param email non-blank, valid e-mail address
     * @param phone 7-15 digit phone number, or blank for none
     * @throws IllegalArgumentException if any value fails validation
     */
    public Customer(String id, String name, String email, String phone) {
        this.id = Validators.requireNonBlank(id, "customer id");
        this.name = Validators.requireNonBlank(name, "customer name");
        this.email = validateEmail(email);
        this.phone = (phone == null || phone.isBlank()) ? "" : validatePhone(phone);
    }

    /**
     * @return the unique customer id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the full name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the e-mail address
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the phone number, or an empty string if the customer has none
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Updates the e-mail address.
     *
     * @param email the new e-mail address
     * @throws IllegalArgumentException if the new address is invalid
     */
    public void setEmail(String email) {
        this.email = validateEmail(email);
    }

    /**
     * Adds a reservation to this customer's history.
     *
     * @param reservation the reservation to add; must not be {@code null}
     */
    public void addReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        reservations.add(reservation);
    }

    /**
     * @return an unmodifiable snapshot of this customer's reservations
     */
    public List<Reservation> getReservations() {
        return List.copyOf(reservations);
    }

    private static String validateEmail(String email) {
        String value = Validators.requireNonBlank(email, "email");
        if (!value.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("invalid email address: " + value);
        }
        return value;
    }

    private static String validatePhone(String phone) {
        if (!phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(
                    "phone number must be 7-15 digits, got: " + phone);
        }
        return phone;
    }
}
