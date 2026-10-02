package com.railway.repository;

import com.railway.exception.SeatNotAvailableException;
import com.railway.model.Payment;
import com.railway.model.Ticket;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for tickets. Booking and cancellation each touch several tables
 * (seats, ticket, passengers, payment), so each is ONE method that runs in ONE
 * database transaction. Never split them across calls.
 */
public interface TicketRepository {

    boolean pnrExists(String pnr);

    /**
     * Atomically: reserve seats, insert the ticket, its passengers and the payment.
     * If the seats are gone, nothing is written.
     *
     * @throws SeatNotAvailableException if fewer seats remain than passengers
     */
    void book(Ticket ticket, Payment payment) throws SeatNotAvailableException;

    Optional<Ticket> findByPnr(String pnr);

    /** Newest first. */
    List<Ticket> findByUser(long userId);

    /**
     * Atomically: store the cancellation already applied to the ticket via
     * Ticket.cancel(...), return the seats, and mark the payment as refunded.
     */
    void saveCancellation(Ticket cancelledTicket);
}