package com.rental.payment;

/**
 * Strategy for taking (and returning) money at checkout.
 *
 * <p>Implementations: {@link CashPayment} and {@link CardPayment}.
 *
 * <p>Open/Closed: a new payment method (wallet, bank transfer, ...) is added
 * by introducing a new implementation — the services, models and UI code that
 * depend on this interface stay untouched.
 */
public interface PaymentMethod {

    /**
     * Charges the given amount.
     *
     * @param amount the amount in dollars; must not be negative
     * @return the amount actually charged
     * @throws com.rental.exception.PaymentDeclinedException if the payment is declined
     */
    double processPayment(double amount);

    /**
     * Returns the given amount to the customer (for example on cancellation).
     *
     * @param amount the amount in dollars; must not be negative
     * @return the amount refunded
     * @throws com.rental.exception.PaymentDeclinedException if the refund cannot be issued
     */
    double refund(double amount);

    /**
     * @return a short human-readable description, e.g. {@code "Card ****4242"}
     */
    String describe();
}
