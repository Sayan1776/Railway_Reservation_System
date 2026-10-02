package com.railway.service;

import com.railway.model.Route;
import com.railway.model.SeatClass;
import com.railway.model.Train;
import com.railway.model.TrainClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Fare = distance in km x the class's rate per km, per passenger, with a minimum charge. */
public class DistanceFareCalculator implements FareCalculator {

    /** Sample value: change it to whatever your project rules say. */
    private static final BigDecimal MIN_FARE_PER_PASSENGER = new BigDecimal("20.00");

    @Override
    public BigDecimal calculate(Train train, SeatClass seatClass, String fromStation,
                                String toStation, int passengers) {
        if (passengers < 1) {
            throw new IllegalArgumentException("passengers must be at least 1");
        }
        Route route = train.getRoute();
        if (route == null) {
            throw new IllegalArgumentException("Train " + train.getTrainNumber() + " has no route");
        }
        TrainClass trainClass = train.getTrainClass(seatClass).orElseThrow(() ->
                new IllegalArgumentException("Train " + train.getTrainNumber() + " has no class " + seatClass.getCode()));

        int km = route.distanceBetween(fromStation, toStation);
        BigDecimal perPassenger = trainClass.getFarePerKm()
                .multiply(BigDecimal.valueOf(km))
                .setScale(2, RoundingMode.HALF_UP)
                .max(MIN_FARE_PER_PASSENGER);
        return perPassenger.multiply(BigDecimal.valueOf(passengers));
    }
}