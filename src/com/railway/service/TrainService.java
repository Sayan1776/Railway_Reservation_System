package com.railway.service;

import com.railway.exception.InvalidInputException;
import com.railway.exception.TrainNotFoundException;
import com.railway.model.Route;
import com.railway.model.RouteStop;
import com.railway.model.SeatClass;
import com.railway.model.Station;
import com.railway.model.Train;
import com.railway.model.TrainClass;
import com.railway.repository.TrainRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TrainService {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    public static final int MAX_ADVANCE_DAYS = 120;

    private final TrainRepository trains;

    public TrainService(TrainRepository trains) {
        this.trains = trains;
    }

    public List<Station> getStations() {
        return trains.findAllStations();
    }

    /** All active trains with route and classes. */
    public List<Train> getAllTrains() {
        return trains.findAll();
    }

    public Train getTrain(int trainNumber) throws TrainNotFoundException {
        return trains.findByNumber(trainNumber)
                .orElseThrow(() -> new TrainNotFoundException("Train " + trainNumber + " not found"));
    }

    public List<Train> search(String from, String to, LocalDate date) throws InvalidInputException {
        String f = requireStation(from);
        String t = requireStation(to);
        if (f.equals(t)) {
            throw new InvalidInputException("From and to stations must differ");
        }
        requireBookableDate(date);
        return trains.searchTrains(f, t, date);
    }

    /** Seats left per class on the given date. */
    public Map<SeatClass, Integer> getAvailability(Train train, LocalDate date) {
        Map<SeatClass, Integer> result = new EnumMap<>(SeatClass.class);
        for (TrainClass tc : train.getClasses()) {
            result.put(tc.getSeatClass(),
                    trains.getAvailableSeats(train.getTrainNumber(), tc.getSeatClass(), date));
        }
        return result;
    }

    // ------------------------------------------------- shared helpers (used by other services)

    static void requireBookableDate(LocalDate date) throws InvalidInputException {
        if (date == null) {
            throw new InvalidInputException("Journey date is required");
        }
        LocalDate today = LocalDate.now(IST);
        if (date.isBefore(today)) {
            throw new InvalidInputException("Journey date must be today or later");
        }
        if (date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new InvalidInputException("Bookings open only " + MAX_ADVANCE_DAYS + " days ahead");
        }
    }

    /**
     * When the train leaves the boarding station. The journey date is the day the train
     * leaves its ORIGIN, so later stops add their day_offset.
     */
    static ZonedDateTime departureOf(Route route, String boardingStation, LocalDate journeyDate) {
        RouteStop stop = route.findStop(boardingStation).orElseThrow(() ->
                new IllegalArgumentException("Station " + boardingStation + " is not on this route"));
        LocalTime time = stop.getDepartureTime();
        if (time == null) {
            throw new IllegalArgumentException("Cannot board at the terminus " + boardingStation);
        }
        return journeyDate.plusDays(stop.getDayOffset()).atTime(time).atZone(IST);
    }

    private String requireStation(String code) throws InvalidInputException {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidInputException("Station code is required");
        }
        String c = code.trim().toUpperCase();
        trains.findStation(c).orElseThrow(() -> new InvalidInputException("Unknown station: " + c));
        return c;
    }
}