package com.rental.service;

import com.rental.exception.DuplicateVehicleIdException;
import com.rental.exception.VehicleNotFoundException;
import com.rental.model.Bike;
import com.rental.model.Car;
import com.rental.model.Truck;
import com.rental.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FleetService}: registration, lookup and the
 * polymorphic fleet reports.
 */
class FleetServiceTest {

    private FleetService fleet;

    @BeforeEach
    void setUp() {
        fleet = new FleetService();
    }

    private void seedFleet() {
        fleet.addVehicle(new Car("VH-001", "Toyota", "Camry", 2023, 45, 5, true));
        fleet.addVehicle(new Car("VH-002", "VW", "Golf", 2021, 38, 5, false));
        fleet.addVehicle(new Bike("VH-003", "Yamaha", "MT-07", 2022, 18, true));
        fleet.addVehicle(new Truck("VH-004", "Ford", "Transit", 2022, 95, 42));
    }

    @Test
    void addAndGetVehicle() {
        Vehicle car = new Car("VH-001", "Toyota", "Camry", 2023, 45, 5, true);
        fleet.addVehicle(car);
        assertSame(car, fleet.getVehicle("VH-001"));
    }

    @Test
    void rejectsDuplicateId() {
        seedFleet();
        assertThrows(DuplicateVehicleIdException.class,
                () -> fleet.addVehicle(new Car("VH-001", "BMW", "320", 2022, 50, 5, true)));
    }

    @Test
    void unknownVehicleThrows() {
        assertThrows(VehicleNotFoundException.class, () -> fleet.getVehicle("VH-999"));
    }

    @Test
    void vehiclesViewIsUnmodifiable() {
        seedFleet();
        assertThrows(UnsupportedOperationException.class,
                () -> fleet.getVehicles().add(new Bike("VH-005", "Honda", "PCX", 2022, 12, false)));
    }

    @Test
    void sizeTracksRegistrations() {
        seedFleet();
        assertEquals(4, fleet.size());
    }

    @Test
    void totalDailyRateSumsAllTypesThroughBaseType() {
        seedFleet();
        assertEquals(196.0, fleet.getTotalDailyRate(), 0.0001); // 45 + 38 + 18 + 95
    }

    @Test
    void fleetReportListsEveryVehicle() {
        seedFleet();
        String report = fleet.getFleetReport();
        assertTrue(report.contains("VH-001"));
        assertTrue(report.contains("VH-004"));
        assertTrue(report.contains("Total daily rate capacity: $196.00"));
    }

    @Test
    void emptyFleetReport() {
        assertEquals("The fleet is empty.", fleet.getFleetReport());
    }
}
