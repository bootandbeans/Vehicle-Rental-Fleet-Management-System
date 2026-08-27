package com.rental.service;

import com.rental.model.Reservation;
import com.rental.model.ReservationObserver;

/**
 * A {@link ReservationObserver} that writes a human-readable notification
 * line to the console. A production system would send an e-mail or SMS here
 * instead — the rest of the application does not need to change.
 */
public final class ConsoleReservationNotifier implements ReservationObserver {

    private static final String TAG = "[NOTIFY]";

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        System.out.printf("%s Reservation %s confirmed: %s rents %s from %s to %s | "
                        + "total $%.2f | paid by %s%n",
                TAG, reservation.getId(), reservation.getCustomer().getName(),
                reservation.getVehicle().getId(), reservation.getStartDate(),
                reservation.getEndDate(), reservation.getTotalCost(),
                reservation.getPaymentMethod().describe());
    }

    @Override
    public void onReservationModified(Reservation reservation) {
        System.out.printf("%s Reservation %s modified: %s's rental now ends %s | "
                        + "new total $%.2f%n",
                TAG, reservation.getId(), reservation.getCustomer().getName(),
                reservation.getEndDate(), reservation.getTotalCost());
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        System.out.printf("%s Reservation %s cancelled: %s refunded $%.2f via %s%n",
                TAG, reservation.getId(), reservation.getCustomer().getName(),
                reservation.getTotalCost(), reservation.getPaymentMethod().describe());
    }
}
