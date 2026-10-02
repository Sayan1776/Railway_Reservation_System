package com.railway.repository.jdbc;

import com.railway.exception.DataAccessException;
import com.railway.model.Route;
import com.railway.model.RouteStop;
import com.railway.model.SeatClass;
import com.railway.model.Station;
import com.railway.model.Train;
import com.railway.model.TrainClass;
import com.railway.repository.TrainRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTrainRepository implements TrainRepository {

    // ---------------------------------------------------------------- stations

    @Override
    public List<Station> findAllStations() {
        String sql = "select station_code, station_name, city from stations order by city, station_name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Station> stations = new ArrayList<>();
            while (rs.next()) {
                stations.add(mapStation(rs));
            }
            return stations;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load stations", e);
        }
    }

    @Override
    public Optional<Station> findStation(String stationCode) {
        String sql = "select station_code, station_name, city from stations where station_code = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, stationCode.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapStation(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load station " + stationCode, e);
        }
    }

    // ------------------------------------------------------------------ trains

    @Override
    public Optional<Train> findByNumber(int trainNumber) {
        try (Connection con = DBConnection.getConnection()) {
            return loadTrain(con, trainNumber);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load train " + trainNumber, e);
        }
    }

    @Override
    public List<Train> findAll() {
        String sql = "select train_number from trains where is_active order by train_number";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            List<Integer> numbers = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    numbers.add(rs.getInt(1));
                }
            }
            return loadAll(con, numbers);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load trains", e);
        }
    }

    @Override
    public List<Train> searchTrains(String fromStationCode, String toStationCode, LocalDate journeyDate) {
        // The date is the day the train leaves its ORIGIN; runs_on is indexed Monday..Sunday.
        String sql =
              "select t.train_number "
            + "  from train_stops a "
            + "  join train_stops b on b.train_number = a.train_number and b.stop_order > a.stop_order "
            + "  join trains t on t.train_number = a.train_number "
            + " where a.station_code = ? and b.station_code = ? "
            + "   and t.is_active "
            + "   and substr(t.runs_on, extract(isodow from cast(? as date))::int, 1) = 'Y' "
            + " order by a.departure_time, t.train_number";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fromStationCode.trim().toUpperCase());
            ps.setString(2, toStationCode.trim().toUpperCase());
            ps.setDate(3, java.sql.Date.valueOf(journeyDate));
            List<Integer> numbers = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    numbers.add(rs.getInt(1));
                }
            }
            return loadAll(con, numbers);
        } catch (SQLException e) {
            throw new DataAccessException("Train search failed", e);
        }
    }

    // ------------------------------------------------------------ availability

    @Override
    public int getAvailableSeats(int trainNumber, SeatClass seatClass, LocalDate journeyDate) {
        // cast(? as date) matters: an untyped parameter in a SELECT list is inferred as text.
        String ensure =
              "insert into seat_availability (train_number, seat_class, journey_date, available_seats) "
            + "select train_number, seat_class, cast(? as date), total_seats "
            + "  from train_classes where train_number = ? and seat_class = ? "
            + "on conflict do nothing";
        String read =
              "select available_seats from seat_availability "
            + " where train_number = ? and seat_class = ? and journey_date = ?";
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(ensure)) {
                ps.setDate(1, java.sql.Date.valueOf(journeyDate));
                ps.setInt(2, trainNumber);
                ps.setString(3, seatClass.getCode());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(read)) {
                ps.setInt(1, trainNumber);
                ps.setString(2, seatClass.getCode());
                ps.setDate(3, java.sql.Date.valueOf(journeyDate));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "Train " + trainNumber + " has no class " + seatClass.getCode());
                    }
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read seat availability", e);
        }
    }

    // ----------------------------------------------------------------- helpers

    private List<Train> loadAll(Connection con, List<Integer> trainNumbers) throws SQLException {
        List<Train> trains = new ArrayList<>();
        for (int number : trainNumbers) {
            loadTrain(con, number).ifPresent(trains::add);
        }
        return trains;
    }

    /** Loads one train, its stops (joined with stations) and its classes on an open connection. */
    private Optional<Train> loadTrain(Connection con, int trainNumber) throws SQLException {
        Train train;
        try (PreparedStatement ps = con.prepareStatement(
                "select train_number, train_name, runs_on, is_active from trains where train_number = ?")) {
            ps.setInt(1, trainNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                train = new Train(rs.getInt("train_number"), rs.getString("train_name"),
                        rs.getString("runs_on"), rs.getBoolean("is_active"));
            }
        }

        String stopSql =
              "select s.stop_order, s.arrival_time, s.departure_time, s.day_offset, s.distance_km, "
            + "       st.station_code, st.station_name, st.city "
            + "  from train_stops s join stations st on st.station_code = s.station_code "
            + " where s.train_number = ? order by s.stop_order";
        List<RouteStop> stops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(stopSql)) {
            ps.setInt(1, trainNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stops.add(new RouteStop(
                            rs.getInt("stop_order"),
                            mapStation(rs),
                            rs.getObject("arrival_time", LocalTime.class),
                            rs.getObject("departure_time", LocalTime.class),
                            rs.getInt("day_offset"),
                            rs.getInt("distance_km")));
                }
            }
        }
        if (stops.size() >= 2) {
            train.setRoute(new Route(trainNumber, stops));
        }

        try (PreparedStatement ps = con.prepareStatement(
                "select seat_class, total_seats, fare_per_km from train_classes where train_number = ?")) {
            ps.setInt(1, trainNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    train.addClass(new TrainClass(trainNumber,
                            SeatClass.fromCode(rs.getString("seat_class")),
                            rs.getInt("total_seats"),
                            rs.getBigDecimal("fare_per_km")));
                }
            }
        }
        return Optional.of(train);
    }

    private Station mapStation(ResultSet rs) throws SQLException {
        return new Station(rs.getString("station_code"), rs.getString("station_name"), rs.getString("city"));
    }
}