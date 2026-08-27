package com.rental.service;

import com.rental.exception.CustomerNotFoundException;
import com.rental.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link CustomerService}: id generation and lookup.
 */
class CustomerServiceTest {

    private CustomerService customers;

    @BeforeEach
    void setUp() {
        customers = new CustomerService();
    }

    @Test
    void registersWithSequentialIds() {
        Customer first = customers.registerCustomer("Anna", "anna@example.com", "");
        Customer second = customers.registerCustomer("Ben", "ben@example.com", "5550101");

        assertEquals("CUST-001", first.getId());
        assertEquals("CUST-002", second.getId());
        assertSame(first, customers.getCustomer("CUST-001"));
        assertSame(second, customers.getCustomer("CUST-002"));
    }

    @Test
    void unknownCustomerThrows() {
        assertThrows(CustomerNotFoundException.class, () -> customers.getCustomer("CUST-999"));
    }

    @Test
    void invalidContactDataIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> customers.registerCustomer("Anna", "not-an-email", ""));
        assertThrows(IllegalArgumentException.class,
                () -> customers.registerCustomer("Anna", "anna@example.com", "12a4"));
    }

    @Test
    void customerListIsExposed() {
        customers.registerCustomer("Anna", "anna@example.com", "");
        customers.registerCustomer("Ben", "ben@example.com", "");
        assertEquals(2, customers.getCustomers().size());
    }
}
