package com.rental.payment;

import com.rental.exception.PaymentDeclinedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CardPayment}: number validation, the per-charge limit
 * and refunds.
 */
class CardPaymentTest {

    @Test
    void rejectsMalformedCardNumbers() {
        assertThrows(IllegalArgumentException.class, () -> new CardPayment(null));
        assertThrows(IllegalArgumentException.class, () -> new CardPayment("424242424242424"));   // 15
        assertThrows(IllegalArgumentException.class, () -> new CardPayment("42424242424242421")); // 17
        assertThrows(IllegalArgumentException.class, () -> new CardPayment("424242424242424a"));
    }

    @Test
    void rejectsInvalidChargeLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new CardPayment("4242424242424242", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CardPayment("4242424242424242", -10));
    }

    @Test
    void processesChargeWithinLimit() {
        CardPayment card = new CardPayment("4242424242424242", 500.0);
        assertEquals(300.0, card.processPayment(300.0), 0.0001);
        assertEquals(500.0, card.processPayment(500.0), 0.0001); // boundary
    }

    @Test
    void declinesChargeAboveLimit() {
        CardPayment card = new CardPayment("4242424242424242", 500.0);
        PaymentDeclinedException ex = assertThrows(PaymentDeclinedException.class,
                () -> card.processPayment(500.01));
        assertTrue(ex.getMessage().contains("exceeds"));
    }

    @Test
    void defaultLimitAllowsLargeCharges() {
        CardPayment card = new CardPayment("4242424242424242");
        assertEquals(9000.0, card.processPayment(9000.0), 0.0001);
        assertThrows(PaymentDeclinedException.class, () -> card.processPayment(20000.0));
    }

    @Test
    void refundsAndDescribes() {
        CardPayment card = new CardPayment("4242424242424242");
        assertEquals(59.0, card.refund(59.0), 0.0001);
        assertThrows(PaymentDeclinedException.class, () -> card.refund(-1));
        assertEquals("Card ****4242", card.describe());
    }
}
