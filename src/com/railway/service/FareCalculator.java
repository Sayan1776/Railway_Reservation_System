package com.railway.service;

import com.railway.model.SeatClass;
import com.railway.model.Train;

import java.math.BigDecimal;

/** Strategy for pricing a booking. Add e.g. a TatkalFareCalculator without touching BookingService. */
public interface FareCalculator {

    /** Total fare for all passengers between two stations of the train's route. */
    BigDecimal calculate(Train train, SeatClass seatClass, String fromStation, String toStation, int passengers);
}