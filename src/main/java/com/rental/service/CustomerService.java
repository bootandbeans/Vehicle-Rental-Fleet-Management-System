package com.rental.service;

import com.rental.exception.CustomerNotFoundException;
import com.rental.model.Customer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Registers and looks up customers.
 *
 * <p>Storage is a {@code Map<String, Customer>} keyed by customer id; ids
 * ({@code CUST-001}, {@code CUST-002}, ...) are generated here, so customer
 * construction stays in one place.
 */
public final class CustomerService {

    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private int nextCustomerNumber = 0;

    /**
     * Registers a new customer and assigns the next customer id.
     *
     * @param name  full name
     * @param email valid e-mail address
     * @param phone 7-15 digit phone number, or blank for none
     * @return the created customer
     * @throws IllegalArgumentException if name, email or phone are invalid
     */
    public Customer registerCustomer(String name, String email, String phone) {
        nextCustomerNumber++;
        String id = String.format(Locale.ROOT, "CUST-%03d", nextCustomerNumber);
        Customer customer = new Customer(id, name, email, phone);
        customers.put(customer.getId(), customer);
        return customer;
    }

    /**
     * Looks up a customer by id.
     *
     * @param customerId the customer id
     * @return the customer with the given id
     * @throws CustomerNotFoundException if no such customer exists
     */
    public Customer getCustomer(String customerId) {
        Objects.requireNonNull(customerId, "customerId");
        Customer customer = customers.get(customerId);
        if (customer == null) {
            throw new CustomerNotFoundException(customerId);
        }
        return customer;
    }

    /**
     * @return an unmodifiable snapshot of all registered customers
     */
    public List<Customer> getCustomers() {
        return List.copyOf(customers.values());
    }
}
