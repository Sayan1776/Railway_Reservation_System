package com.railway.model;

import java.math.BigDecimal;

/** A class (SL, 3A, ...) offered on one train: capacity and price per km. */
public class TrainClass {

    private final int trainNumber;
    private final SeatClass seatClass;
    private int totalSeats;
    private BigDecimal farePerKm;

    public TrainClass(int trainNumber, SeatClass seatClass, int totalSeats, BigDecimal farePerKm) {
        this.trainNumber = trainNumber;
        this.seatClass = Require.notNull(seatClass, "seatClass");
        this.totalSeats = Require.atLeast(totalSeats, 1, "totalSeats");
        this.farePerKm = requirePositive(farePerKm);
    }

    public int getTrainNumber() { return trainNumber; }
    public SeatClass getSeatClass() { return seatClass; }
    public int getTotalSeats() { return totalSeats; }
    public BigDecimal getFarePerKm() { return farePerKm; }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = Require.atLeast(totalSeats, 1, "totalSeats");
    }

    public void setFarePerKm(BigDecimal farePerKm) {
        this.farePerKm = requirePositive(farePerKm);
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        Require.notNull(value, "farePerKm");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("farePerKm must be positive");
        }
        return value;
    }

    @Override
    public String toString() {
        return seatClass.getCode() + " x" + totalSeats + " @" + farePerKm + "/km";
    }
}