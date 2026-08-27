package com.rental.payment;

import com.rental.exception.PaymentDeclinedException;

/**
 * Payment in cash: every non-negative amount is accepted at the counter.
 */
public final class CashPayment implements PaymentMethod {

    @Override
    public double processPayment(double amount) {
        if (amount < 0) {
            throw new PaymentDeclinedException("cash amount cannot be negative: " + amount);
        }
        return amount;
    }

    @Override
    public double refund(double amount) {
        if (amount < 0) {
            throw new PaymentDeclinedException("cash refund cannot be negative: " + amount);
        }
        return amount;
    }

    @Override
    public String describe() {
        return "Cash";
    }
}
