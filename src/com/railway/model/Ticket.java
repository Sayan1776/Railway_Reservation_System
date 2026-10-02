package com.railway.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Ticket {

    private final String pnr;               // 10 digits
    private final long userId;
    private final int trainNumber;
    private final SeatClass seatClass;
    private final LocalDate journeyDate;
    private final String fromStation;       // station codes
    private final String toStation;
    private BookingStatus status;
    private final BigDecimal totalFare;
    private BigDecimal refundAmount;        // null until cancelled
    private final LocalDateTime bookedAt;   // null until loaded from the database
    private LocalDateTime cancelledAt;
    private final List<Passenger> passengers = new ArrayList<>();

    public Ticket(String pnr, long userId, int trainNumber, SeatClass seatClass,
                  LocalDate journeyDate, String fromStation, String toStation,
                  BookingStatus status, BigDecimal totalFare,
                  BigDecimal refundAmount, LocalDateTime bookedAt, LocalDateTime cancelledAt) {
        this.pnr = Require.text(pnr, "pnr");
        if (!this.pnr.matches("[0-9]{10}")) {
            throw new IllegalArgumentException("pnr must be exactly 10 digits");
        }
        this.userId = userId;
        this.trainNumber = trainNumber;
        this.seatClass = Require.notNull(seatClass, "seatClass");
        this.journeyDate = Require.notNull(journeyDate, "journeyDate");
        this.fromStation = Require.text(fromStation, "fromStation").toUpperCase();
        this.toStation = Require.text(toStation, "toStation").toUpperCase();
        if (this.fromStation.equals(this.toStation)) {
            throw new IllegalArgumentException("from and to stations must differ");
        }
        this.status = Require.notNull(status, "status");
        this.totalFare = Require.notNull(totalFare, "totalFare");
        if (totalFare.signum() < 0) {
            throw new IllegalArgumentException("totalFare must not be negative");
        }
        this.refundAmount = refundAmount;
        this.bookedAt = bookedAt;
        this.cancelledAt = cancelledAt;
    }

    public String getPnr() { return pnr; }
    public long getUserId() { return userId; }
    public int getTrainNumber() { return trainNumber; }
    public SeatClass getSeatClass() { return seatClass; }
    public LocalDate getJourneyDate() { return journeyDate; }
    public String getFromStation() { return fromStation; }
    public String getToStation() { return toStation; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getTotalFare() { return totalFare; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }

    public List<Passenger> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }

    public int getPassengerCount() {
        return passengers.size();
    }

    public void addPassenger(Passenger passenger) {
        Require.notNull(passenger, "passenger");
        passenger.setPnr(pnr);
        passengers.add(passenger);
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    /** The refund amount is calculated by CancellationService; the ticket just records it. */
    public void cancel(BigDecimal refund, LocalDateTime when) {
        if (isCancelled()) {
            throw new IllegalStateException("Ticket " + pnr + " is already cancelled");
        }
        Require.notNull(refund, "refund");
        if (refund.signum() < 0 || refund.compareTo(totalFare) > 0) {
            throw new IllegalArgumentException("refund must be between 0 and the total fare");
        }
        this.status = BookingStatus.CANCELLED;
        this.refundAmount = refund;
        this.cancelledAt = Require.notNull(when, "when");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket)) return false;
        return pnr.equals(((Ticket) o).pnr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pnr);
    }

    @Override
    public String toString() {
        return "PNR " + pnr + " | Train " + trainNumber + " | " + fromStation + " -> " + toStation
                + " | " + journeyDate + " | " + seatClass.getCode() + " | " + status;
    }
}