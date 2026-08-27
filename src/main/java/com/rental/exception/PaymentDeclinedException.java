package com.rental.exception;

/**
 * Thrown when a payment (or refund) cannot be processed, for example because
 * a card charge exceeds the card's limit or an amount is negative.
 *
 * <p>Unchecked because a declined payment is a local input/state problem the
 * calling code can recover from by choosing another payment method; it does
 * not require every caller in the stack to be compiled against it.
 */
public class PaymentDeclinedException extends RentalException {

    /**
     * Creates a new payment-declined exception.
     *
     * @param reason human-readable explanation of why the payment failed
     */
    public PaymentDeclinedException(String reason) {
        super(reason);
    }
}
