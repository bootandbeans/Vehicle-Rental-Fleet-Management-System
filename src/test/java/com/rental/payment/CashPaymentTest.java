package com.rental.payment;

import com.rental.exception.PaymentDeclinedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link CashPayment}.
 */
class CashPaymentTest {

    private final CashPayment payment = new CashPayment();

    @Test
    void processesAnyNonNegativeAmount() {
        assertEquals(120.5, payment.processPayment(120.5), 0.0001);
        assertEquals(0.0, payment.processPayment(0.0), 0.0001);
    }

    @Test
    void refundsAnyNonNegativeAmount() {
        assertEquals(80.0, payment.refund(80.0), 0.0001);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThrows(PaymentDeclinedException.class, () -> payment.processPayment(-1));
        assertThrows(PaymentDeclinedException.class, () -> payment.refund(-0.01));
    }

    @Test
    void describesItself() {
        assertEquals("Cash", payment.describe());
    }
}
