package com.railway.service;

import com.railway.exception.InvalidInputException;
import com.railway.exception.InvalidPNRException;
import com.railway.model.Person;
import com.railway.model.Ticket;
import com.railway.model.Train;
import com.railway.repository.TicketRepository;
import com.railway.repository.TrainRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class CancellationService {

    private final TrainRepository trains;
    private final TicketRepository tickets;

    public CancellationService(TrainRepository trains, TicketRepository tickets) {
        this.trains = trains;
        this.tickets = tickets;
    }

    /** Owner or admin cancels a confirmed ticket; the refund depends on how close departure is. */
    public Ticket cancel(Person requester, String pnr) throws InvalidPNRException, InvalidInputException {
        String key = pnr == null ? "" : pnr.trim();

        // Same message whether the PNR is missing or belongs to someone else: don't leak existence.
        Ticket ticket = tickets.findByPnr(key)
                .filter(t -> requester.isAdmin()
                        || (requester.getId() != null && t.getUserId() == requester.getId()))
                .orElseThrow(() -> new InvalidPNRException("No ticket " + key + " found for this account"));

        if (ticket.isCancelled()) {
            throw new InvalidInputException("Ticket " + key + " is already cancelled");
        }

        Train train = trains.findByNumber(ticket.getTrainNumber())
                .orElseThrow(() -> new IllegalStateException("Train " + ticket.getTrainNumber() + " is missing"));
        if (train.getRoute() == null) {
            throw new IllegalStateException("Train " + ticket.getTrainNumber() + " has no route");
        }

        ZonedDateTime departure = TrainService.departureOf(
                train.getRoute(), ticket.getFromStation(), ticket.getJourneyDate());
        Duration untilDeparture = Duration.between(Instant.now(), departure.toInstant());
        if (untilDeparture.isNegative()) {
            throw new InvalidInputException("The train has already departed; this ticket can no longer be cancelled");
        }

        BigDecimal refund = ticket.getTotalFare()
                .multiply(BigDecimal.valueOf(refundPercent(untilDeparture.toHours())))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        ticket.cancel(refund, LocalDateTime.now());
        tickets.saveCancellation(ticket);
        return ticket;
    }

    /**
     * SAMPLE refund policy, not real railway rules: edit the tiers to match your project brief.
     * Hours are counted from now until the train leaves the boarding station.
     */
    static int refundPercent(long hoursBeforeDeparture) {
        if (hoursBeforeDeparture >= 48) return 90;
        if (hoursBeforeDeparture >= 12) return 50;
        if (hoursBeforeDeparture >= 4) return 25;
        return 0;
    }
}