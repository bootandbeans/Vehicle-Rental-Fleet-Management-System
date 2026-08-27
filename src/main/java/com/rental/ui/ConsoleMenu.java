package com.rental.ui;

import com.rental.RentalApp;
import com.rental.demo.DemoRunner;
import com.rental.exception.RentalException;
import com.rental.exception.VehicleUnavailableException;
import com.rental.model.Customer;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.payment.CardPayment;
import com.rental.payment.CashPayment;
import com.rental.payment.PaymentMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Interactive console menu for the rental system.
 *
 * <p>Each option maps to a small handler method; domain errors are caught
 * at the menu level and printed instead of crashing the loop.
 */
public final class ConsoleMenu {

    private static final String MENU = """
            -- What would you like to do? --
             1) Show fleet
             2) Add a vehicle
             3) Register a customer
             4) Reserve a vehicle
             5) Show reservations
             6) Modify a reservation (new end date)
             7) Cancel a reservation
             8) Vehicles available for a date range
             9) Pricing comparison (3 / 10 / 20 days)
            10) Run scripted demo scenarios
             0) Exit
            """;

    private final RentalApp app;
    private final Scanner scanner;

    /**
     * Creates the menu.
     *
     * @param app     the application
     * @param scanner input source (System.in in the real app)
     */
    public ConsoleMenu(RentalApp app, Scanner scanner) {
        this.app = Objects.requireNonNull(app, "app");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    /**
     * Runs the menu loop until the user picks exit.
     */
    public void run() {
        System.out.println(MENU);
        while (true) {
            System.out.print("> ");
            String choice = nextLine();
            if (choice.equals("0")) {
                System.out.println("Goodbye!");
                return;
            }
            try {
                switch (choice) {
                    case "1" -> showFleet();
                    case "2" -> handleAddVehicle();
                    case "3" -> handleRegisterCustomer();
                    case "4" -> handleReserve();
                    case "5" -> handleShowReservations();
                    case "6" -> handleModifyReservation();
                    case "7" -> handleCancelReservation();
                    case "8" -> handleAvailabilityReport();
                    case "9" -> handlePricingComparison();
                    case "10" -> new DemoRunner(app).run();
                    default -> System.out.println("Unknown option: '" + choice + "'");
                }
            } catch (RentalException | VehicleUnavailableException
                         | IllegalArgumentException ex) {
                // IllegalArgumentException also covers NumberFormatException and
                // DateTimeParseException from the input parsing helpers.
                System.out.println("x " + ex.getMessage());
            }
        }
    }

    private void showFleet() {
        System.out.println(app.getFleetReport());
    }

    private void handleAddVehicle() {
        String choice = ask("Vehicle type (1=Car, 2=Bike, 3=Truck): ");
        switch (choice) {
            case "1" -> addCar();
            case "2" -> addBike();
            case "3" -> addTruck();
            default -> System.out.println("Unknown vehicle type: '" + choice + "'");
        }
    }

    private void addCar() {
        String make = ask("Make: ");
        String model = ask("Model: ");
        int year = askInt("Year: ");
        double rate = askDouble("Daily rate: ");
        int seats = askInt("Seats: ");
        boolean airConditioned = ask("Air-conditioned? (y/n): ").equalsIgnoreCase("y");
        System.out.println("Added: "
                + app.addCar(make, model, year, rate, seats, airConditioned).getDetails());
    }

    private void addBike() {
        String make = ask("Make: ");
        String model = ask("Model: ");
        int year = askInt("Year: ");
        double rate = askDouble("Daily rate: ");
        boolean helmet = ask("Helmet add-on required? (y/n): ").equalsIgnoreCase("y");
        System.out.println("Added: "
                + app.addBike(make, model, year, rate, helmet).getDetails());
    }

    private void addTruck() {
        String make = ask("Make: ");
        String model = ask("Model: ");
        int year = askInt("Year: ");
        double rate = askDouble("Daily rate: ");
        double cargo = askDouble("Cargo capacity (m3): ");
        System.out.println("Added: "
                + app.addTruck(make, model, year, rate, cargo).getDetails());
    }

    private void handleRegisterCustomer() {
        String name = ask("Full name: ");
        String email = ask("Email: ");
        String phone = ask("Phone (digits, blank ok): ");
        Customer customer = app.registerCustomer(name, email, phone);
        System.out.println("Registered: " + customer.getId() + " " + customer.getName());
    }

    private void handleReserve() throws VehicleUnavailableException {
        System.out.println("Customers:");
        for (Customer customer : app.getCustomers()) {
            System.out.println("  " + customer.getId() + "  " + customer.getName());
        }
        System.out.println("Fleet:");
        for (Vehicle vehicle : app.getFleetService().getVehicles()) {
            System.out.println("  " + vehicle.getDetails());
        }
        String customerId = ask("Customer id: ");
        String vehicleId = ask("Vehicle id: ");
        LocalDate start = askDate("Start date");
        LocalDate end = askDate("End date");
        Reservation reservation = app.reserve(customerId, vehicleId, start, end,
                askPaymentMethod());
        System.out.printf("Reservation %s confirmed | total $%.2f%n",
                reservation.getId(), reservation.getTotalCost());
    }

    private PaymentMethod askPaymentMethod() {
        String choice = ask("Payment (1=Cash, 2=Card): ");
        if (choice.equals("2")) {
            String number = ask("16-digit card number: ");
            return new CardPayment(number);
        }
        return new CashPayment();
    }

    private void handleShowReservations() {
        List<Reservation> all = app.getReservations();
        if (all.isEmpty()) {
            System.out.println("No reservations yet.");
            return;
        }
        for (Reservation reservation : all) {
            System.out.printf("  %s | %s | %s | %s -> %s | $%.2f | %s | %s%n",
                    reservation.getId(), reservation.getCustomer().getName(),
                    reservation.getVehicle().getId(), reservation.getStartDate(),
                    reservation.getEndDate(), reservation.getTotalCost(),
                    reservation.getPaymentMethod().describe(), reservation.getStatus());
        }
    }

    private void handleModifyReservation() throws VehicleUnavailableException {
        String id = ask("Reservation id: ");
        LocalDate newEnd = askDate("New end date");
        Reservation reservation = app.modifyReservation(id, newEnd);
        System.out.printf("Updated %s: new end date %s | total $%.2f%n",
                reservation.getId(), reservation.getEndDate(), reservation.getTotalCost());
    }

    private void handleCancelReservation() {
        String id = ask("Reservation id: ");
        app.cancelReservation(id);
        System.out.println("Reservation " + id + " cancelled and refunded.");
    }

    private void handleAvailabilityReport() {
        LocalDate start = askDate("Start date");
        LocalDate end = askDate("End date");
        List<Vehicle> free = app.findAvailableVehicles(start, end);
        if (free.isEmpty()) {
            System.out.println("No vehicles are free for that range.");
            return;
        }
        System.out.println("Available vehicles:");
        for (Vehicle vehicle : free) {
            System.out.println("  " + vehicle.getDetails());
        }
    }

    private void handlePricingComparison() {
        System.out.printf("%-52s %9s %9s %9s%n", "Vehicle", "3 days", "10 days", "20 days");
        for (Vehicle vehicle : app.getFleetService().getVehicles()) {
            System.out.printf("%-52s %9.2f %9.2f %9.2f%n", vehicle.getDetails(),
                    vehicle.calculateRentalCost(3), vehicle.calculateRentalCost(10),
                    vehicle.calculateRentalCost(20));
        }
    }

    // -- input helpers --------------------------------------------------------

    private String nextLine() {
        return scanner.nextLine().trim();
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return nextLine();
    }

    private int askInt(String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(nextLine());
    }

    private double askDouble(String prompt) {
        System.out.print(prompt);
        return Double.parseDouble(nextLine());
    }

    private LocalDate askDate(String prompt) {
        System.out.print(prompt + " (yyyy-MM-dd): ");
        return LocalDate.parse(nextLine());
    }
}
