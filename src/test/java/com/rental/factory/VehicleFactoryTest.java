package com.rental.factory;

import com.rental.model.Bike;
import com.rental.model.Car;
import com.rental.model.Truck;
import com.rental.model.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link VehicleFactory}: id generation, subtype creation and
 * validation pass-through.
 */
class VehicleFactoryTest {

    @Test
    void assignsSequentialIds() {
        VehicleFactory factory = new VehicleFactory();
        Vehicle car = factory.createCar("Toyota", "Camry", 2023, 40, 5, true);
        Vehicle bike = factory.createBike("Yamaha", "MT-07", 2022, 15, true);
        Vehicle truck = factory.createTruck("Ford", "Transit", 2022, 90, 40);

        assertEquals("VH-001", car.getId());
        assertEquals("VH-002", bike.getId());
        assertEquals("VH-003", truck.getId());
    }

    @Test
    void peekNextIdDoesNotConsume() {
        VehicleFactory factory = new VehicleFactory();
        assertEquals("VH-001", factory.peekNextId());
        assertEquals("VH-001", factory.peekNextId());
        Vehicle car = factory.createCar("Toyota", "Camry", 2023, 40, 5, true);
        assertEquals("VH-001", car.getId());
        assertEquals("VH-002", factory.peekNextId());
    }

    @Test
    void createsTheRightSubtype() {
        VehicleFactory factory = new VehicleFactory();
        assertTrue(factory.createCar("Toyota", "Camry", 2023, 40, 5, true) instanceof Car);
        assertTrue(factory.createBike("Yamaha", "MT-07", 2022, 15, false) instanceof Bike);
        assertTrue(factory.createTruck("Ford", "Transit", 2022, 90, 40) instanceof Truck);
    }

    @Test
    void propagatesValidationErrors() {
        VehicleFactory factory = new VehicleFactory();
        assertThrows(IllegalArgumentException.class,
                () -> factory.createCar("Toyota", "Camry", 2023, -1, 5, true));
        assertThrows(IllegalArgumentException.class,
                () -> factory.createCar("Toyota", "Camry", 2023, 40, 99, true));
        assertThrows(IllegalArgumentException.class,
                () -> factory.createTruck("Ford", "Transit", 2022, 90, -5));
    }
}
