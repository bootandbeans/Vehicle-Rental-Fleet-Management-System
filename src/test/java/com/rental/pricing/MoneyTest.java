package com.rental.pricing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link Money#roundToCents(double)}.
 */
class MoneyTest {

    @Test
    void roundsDown() {
        assertEquals(1.23, Money.roundToCents(1.234), 0.0001);
    }

    @Test
    void roundsUp() {
        assertEquals(1.24, Money.roundToCents(1.239), 0.0001);
    }

    @Test
    void keepsWholeCents() {
        assertEquals(100.0, Money.roundToCents(100.0), 0.0001);
        assertEquals(252.0, Money.roundToCents(251.9999999), 0.0001);
    }
}
