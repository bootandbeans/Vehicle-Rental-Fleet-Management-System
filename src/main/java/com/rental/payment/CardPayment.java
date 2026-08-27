package com.rental.payment;

import com.rental.exception.PaymentDeclinedException;

/**
 * Payment by bank card.
 *
 * <p>The card is modelled by a 16-digit number and a per-charge limit: any
 * single charge above the limit is declined, which lets the demo and the
 * tests exercise the failure path deterministically.
 */
public final class CardPayment implements PaymentMethod {

    private static final int CARD_NUMBER_LENGTH = 16;
    private static final double DEFAULT_MAX_SINGLE_CHARGE = 10_000.0;

    private final String cardNumber;
    private final double maxSingleCharge;

    /**
     * Creates a card payment with the default per-charge limit of $10,000.
     *
     * @param cardNumber exactly 16 digits
     * @throws IllegalArgumentException if the card number is not 16 digits
     */
    public CardPayment(String cardNumber) {
        this(cardNumber, DEFAULT_MAX_SINGLE_CHARGE);
    }

    /**
     * Creates a card payment with an explicit per-charge limit.
     *
     * @param cardNumber      exactly 16 digits
     * @param maxSingleCharge maximum amount a single charge may reach
     * @throws IllegalArgumentException if the card number or limit is invalid
     */
    public CardPayment(String cardNumber, double maxSingleCharge) {
        if (cardNumber == null || !cardNumber.matches("\\d{" + CARD_NUMBER_LENGTH + "}")) {
            throw new IllegalArgumentException(
                    "card number must be exactly 16 digits, got: " + cardNumber);
        }
        if (maxSingleCharge <= 0) {
            throw new IllegalArgumentException(
                    "max single charge must be positive, got: " + maxSingleCharge);
        }
        this.cardNumber = cardNumber;
        this.maxSingleCharge = maxSingleCharge;
    }

    @Override
    public double processPayment(double amount) {
        if (amount < 0) {
            throw new PaymentDeclinedException("card charge cannot be negative: " + amount);
        }
        if (amount > maxSingleCharge) {
            throw new PaymentDeclinedException(String.format(
                    "charge $%.2f exceeds the card limit of $%.2f", amount, maxSingleCharge));
        }
        return amount;
    }

    @Override
    public double refund(double amount) {
        if (amount < 0) {
            throw new PaymentDeclinedException("card refund cannot be negative: " + amount);
        }
        return amount;
    }

    @Override
    public String describe() {
        return "Card ****" + cardNumber.substring(CARD_NUMBER_LENGTH - 4);
    }
}
