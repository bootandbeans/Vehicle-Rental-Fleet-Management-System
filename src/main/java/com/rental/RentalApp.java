package com.rental;

import com.rental.exception.VehicleUnavailableException;
import com.rental.factory.VehicleFactory;
import com.rental.model.Bike;
import com.rental.model.Car;
import com.rental.model.Customer;
import com.rental.model.Reservation;
import com.rental.model.Truck;
import com.rental.model.Vehicle;
import com.rental.payment.PaymentMethod;
import com.rental.service.ConsoleReservationNotifier;
import com.rental.service.CustomerService;
import com.rental.service.FleetService;
import com.rental.service.ReservationService;

import java.time.LocalDate;
import java.util.List;

/**
 * Composition root of the rental system.
 *
 * <p>Wires the services together, registers the console notifier, and
 * exposes the high-level operations used by the UI and the demo. Keeping
 * the wiring here means the UI never touches service internals directly.
 */
public final class RentalApp {

    private final FleetService fleetService = new FleetService();
    private final CustomerService customerService = new CustomerService();
    private final ReservationService reservationService;
    private final VehicleFactory vehicleFactory = new VehicleFactory();

    private RentalApp() {
        this.reservationService = new ReservationService(fleetService, customerService);
        this.reservationService.addObserver(new ConsoleReservationNotifier());
    }

    /**
     * Creates the app pre-seeded with a small sample fleet and customers.
     *
     * @return a ready-to-use app
     */
    public static RentalApp createWithSampleData() {
        RentalApp app = new RentalApp();
        app.seedSampleData();
        return app;
    }

    /**
     * Creates an empty app (no vehicles, no customers).
     *
     * @return an empty app
     */
    public static RentalApp createEmpty() {
        return new RentalApp();
    }

    // -- registration -------------------------------------------------------

    /**
     * Registers a customer.
     *
     * @param name  full name
     * @param email e-mail address
     * @param phone phone number or blank
     * @return the created customer
     */
    public Customer registerCustomer(String name, String email, String phone) {
        return customerService.registerCustomer(name, email, phone);
    }

    /**
     * Creates a car via the {@link VehicleFactory} and adds it to the fleet.
     *
     * @return the created car
     */
    public Car addCar(String make, String model, int year, double dailyRate,
                      int seats, boolean hasAirConditioning) {
        Car car = vehicleFactory.createCar(make, model, year, dailyRate, seats, hasAirConditioning);
        fleetService.addVehicle(car);
        return car;
    }

    /**
     * Creates a bike via the {@link VehicleFactory} and adds it to the fleet.
     *
     * @return the created bike
     */
    public Bike addBike(String make, String model, int year, double dailyRate,
                        boolean requiresHelmetAddOn) {
        Bike bike = vehicleFactory.createBike(make, model, year, dailyRate, requiresHelmetAddOn);
        fleetService.addVehicle(bike);
        return bike;
    }

    /**
     * Creates a truck via the {@link VehicleFactory} and adds it to the fleet.
     *
     * @return the created truck
     */
    public Truck addTruck(String make, String model, int year, double dailyRate,
                          double cargoCapacityCubicMeters) {
        Truck truck = vehicleFactory.createTruck(make, model, year, dailyRate,
                cargoCapacityCubicMeters);
        fleetService.addVehicle(truck);
        return truck;
    }

    // -- reservations ---------------------------------------------------------

    /**
     * Reserves a vehicle for a customer.
     *
     * @return the confirmed reservation
     * @throws VehicleUnavailableException if the vehicle is already booked
     */
    public Reservation reserve(String customerId, String vehicleId, LocalDate startDate,
                               LocalDate endDate, PaymentMethod paymentMethod)
            throws VehicleUnavailableException {
        return reservationService.reserve(customerId, vehicleId, startDate, endDate, paymentMethod);
    }

    /**
     * Changes the end date of a confirmed reservation.
     *
     * @return the updated reservation
     * @throws VehicleUnavailableException if the new range conflicts
     */
    public Reservation modifyReservation(String reservationId, LocalDate newEndDate)
            throws VehicleUnavailableException {
        return reservationService.modifyReservation(reservationId, newEndDate);
    }

    /**
     * Cancels a confirmed reservation and refunds it.
     *
     * @param reservationId the reservation to cancel
     */
    public void cancelReservation(String reservationId) {
        reservationService.cancelReservation(reservationId);
    }

    // -- queries ----------------------------------------------------------------

    /**
     * @return the vehicles free for the given range
     */
    public List<Vehicle> findAvailableVehicles(LocalDate startDate, LocalDate endDate) {
        return reservationService.findAvailableVehicles(startDate, endDate);
    }

    /**
     * @return all reservations in the system
     */
    public List<Reservation> getReservations() {
        return reservationService.getAllReservations();
    }

    /**
     * @param customerId an existing customer id
     * @return that customer's reservations
     */
    public List<Reservation> getReservationsForCustomer(String customerId) {
        return reservationService.getReservationsForCustomer(customerId);
    }

    /**
     * @return all registered customers
     */
    public List<Customer> getCustomers() {
        return customerService.getCustomers();
    }

    /**
     * @return a text report of the whole fleet
     */
    public String getFleetReport() {
        return fleetService.getFleetReport();
    }

    /**
     * @return the fleet service (for direct fleet queries from the UI)
     */
    public FleetService getFleetService() {
        return fleetService;
    }

    /**
     * @return the customer service
     */
    public CustomerService getCustomerService() {
        return customerService;
    }

    /**
     * @return the reservation service
     */
    public ReservationService getReservationService() {
        return reservationService;
    }

    private void seedSampleData() {
        addCar("Toyota", "Camry", 2023, 45.0, 5, true);
        addCar("VW", "Golf", 2021, 38.0, 5, false);
        addBike("Yamaha", "MT-07", 2022, 18.0, true);
        addTruck("Ford", "Transit", 2022, 95.0, 42.0);
        registerCustomer("Anna Kowalski", "anna.kowalski@example.com", "5550101");
        registerCustomer("Ben Carter", "ben.carter@example.com", "5550202");
    }
}
