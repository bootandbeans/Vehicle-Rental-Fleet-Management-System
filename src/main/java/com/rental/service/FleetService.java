package com.rental.service;

import com.rental.exception.DuplicateVehicleIdException;
import com.rental.exception.VehicleNotFoundException;
import com.rental.model.Vehicle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the fleet: registration, lookup and fleet-wide reports.
 *
 * <p>Storage is a {@code Map<String, Vehicle>} keyed by vehicle id. All
 * reports are computed through the base {@link Vehicle} type only
 * (polymorphism) — no subtype checks anywhere.
 */
public final class FleetService {

    private final Map<String, Vehicle> fleet = new LinkedHashMap<>();

    /**
     * Registers a vehicle in the fleet.
     *
     * @param vehicle the vehicle to add; must not be {@code null}
     * @throws DuplicateVehicleIdException if the fleet already contains this id
     */
    public void addVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        if (fleet.containsKey(vehicle.getId())) {
            throw new DuplicateVehicleIdException(vehicle.getId());
        }
        fleet.put(vehicle.getId(), vehicle);
    }

    /**
     * Looks up a vehicle by id.
     *
     * @param vehicleId the fleet id
     * @return the vehicle with the given id
     * @throws VehicleNotFoundException if no such vehicle exists
     */
    public Vehicle getVehicle(String vehicleId) {
        Objects.requireNonNull(vehicleId, "vehicleId");
        Vehicle vehicle = fleet.get(vehicleId);
        if (vehicle == null) {
            throw new VehicleNotFoundException(vehicleId);
        }
        return vehicle;
    }

    /**
     * @return an unmodifiable snapshot of all vehicles in the fleet
     */
    public List<Vehicle> getVehicles() {
        return List.copyOf(fleet.values());
    }

    /**
     * @return the number of vehicles in the fleet
     */
    public int size() {
        return fleet.size();
    }

    /**
     * Sum of the fleet's daily rates — a coarse "revenue capacity" metric
     * computed purely through the base type.
     *
     * @return the total of all daily rates in dollars
     */
    public double getTotalDailyRate() {
        return fleet.values().stream()
                .mapToDouble(Vehicle::getDailyRate)
                .sum();
    }

    /**
     * Builds a text report of the whole fleet, one line per vehicle via
     * {@link Vehicle#getDetails()}.
     *
     * @return a multi-line fleet report
     */
    public String getFleetReport() {
        if (fleet.isEmpty()) {
            return "The fleet is empty.";
        }
        StringBuilder report = new StringBuilder();
        report.append("Fleet (").append(fleet.size()).append(" vehicles):\n");
        for (Vehicle vehicle : fleet.values()) {
            report.append("  ").append(vehicle.getDetails()).append('\n');
        }
        report.append(String.format("Total daily rate capacity: $%.2f", getTotalDailyRate()));
        return report.toString();
    }
}
