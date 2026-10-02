package com.railway.repository;

import com.railway.model.SeatClass;
import com.railway.model.Station;
import com.railway.model.Train;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Read access to stations, trains and seat availability. Throws DataAccessException on DB failure. */
public interface TrainRepository {

    List<Station> findAllStations();

    Optional<Station> findStation(String stationCode);

    /** Loads the train with its route and classes. */
    Optional<Train> findByNumber(int trainNumber);

    /** All active trains, each with route and classes. */
    List<Train> findAll();

    /**
     * Active trains that run on the given date and go from one station to a later
     * station on their route. Each train comes back with route and classes loaded.
     */
    List<Train> searchTrains(String fromStationCode, String toStationCode, LocalDate journeyDate);

    /**
     * Seats left for a train, class and date. Creates the availability row
     * (full capacity) the first time a date is looked up.
     *
     * @throws IllegalArgumentException if the train does not have that class
     */
    int getAvailableSeats(int trainNumber, SeatClass seatClass, LocalDate journeyDate);
}