package com.rental.model;

/**
 * Observer (listener) for reservation life-cycle events.
 *
 * <p>Implementations receive a callback when a reservation is confirmed,
 * modified or cancelled — typically to send an e-mail, an SMS, or (in this
 * project) a console notification. The
 * {@code com.rental.service.ReservationService} keeps a set of observers and
 * notifies all of them after each event.
 *
 * <p>Adding a notification channel (e-mail service, push notification, ...)
 * is a matter of implementing this interface and registering it — no
 * reservation logic changes.
 */
public interface ReservationObserver {

    /**
     * Called after a reservation has been confirmed and paid.
     *
     * @param reservation the confirmed reservation
     */
    void onReservationConfirmed(Reservation reservation);

    /**
     * Called after a confirmed reservation's date range (and possibly cost)
     * has changed.
     *
     * @param reservation the modified reservation
     */
    void onReservationModified(Reservation reservation);

    /**
     * Called after a reservation has been cancelled and refunded.
     *
     * @param reservation the cancelled reservation
     */
    void onReservationCancelled(Reservation reservation);
}
