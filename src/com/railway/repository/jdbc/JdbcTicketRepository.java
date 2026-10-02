package com.railway.repository.jdbc;

import com.railway.exception.DataAccessException;
import com.railway.exception.SeatNotAvailableException;
import com.railway.model.BookingStatus;
import com.railway.model.Passenger;
import com.railway.model.Payment;
import com.railway.model.SeatClass;
import com.railway.model.Ticket;
import com.railway.repository.TicketRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTicketRepository implements TicketRepository {

    private static final String TICKET_COLUMNS =
          "pnr, user_id, train_number, seat_class, journey_date, from_station, to_station, "
        + "status, total_fare, refund_amount, booked_at, cancelled_at";

    // ------------------------------------------------------------------- lookups

    @Override
    public boolean pnrExists(String pnr) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("select 1 from tickets where pnr = ?")) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check PNR " + pnr, e);
        }
    }

    @Override
    public Optional<Ticket> findByPnr(String pnr) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "select " + TICKET_COLUMNS + " from tickets where pnr = ?")) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Ticket ticket = mapTicket(rs);
                loadPassengers(con, ticket);
                return Optional.of(ticket);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load ticket " + pnr, e);
        }
    }

    @Override
    public List<Ticket> findByUser(long userId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "select " + TICKET_COLUMNS + " from tickets where user_id = ? order by booked_at desc")) {
            ps.setLong(1, userId);
            List<Ticket> tickets = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapTicket(rs));
                }
            }
            for (Ticket t : tickets) {
                loadPassengers(con, t);
            }
            return tickets;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load tickets for user " + userId, e);
        }
    }

    // ------------------------------------------------------------------- booking

    @Override
    public void book(Ticket ticket, Payment payment) throws SeatNotAvailableException {
        if (ticket.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only CONFIRMED tickets can be booked (waitlist is not implemented)");
        }
        if (ticket.getPassengerCount() == 0) {
            throw new IllegalArgumentException("A ticket needs at least one passenger");
        }
        if (!payment.getPnr().equals(ticket.getPnr())) {
            throw new IllegalArgumentException("Payment belongs to a different PNR");
        }
        if (payment.getStatus() != Payment.Status.SUCCESS) {
            throw new IllegalArgumentException("Only a successful payment can book a ticket");
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                reserveSeats(con, ticket);
                insertTicket(con, ticket);
                insertPassengers(con, ticket);
                insertPayment(con, payment);
                con.commit();
            } catch (SeatNotAvailableException | RuntimeException e) {
                con.rollback();
                throw e;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Booking failed for PNR " + ticket.getPnr(), e);
        }
    }

    /**
     * The atomic UPDATE is what prevents double booking: Postgres locks the row, so two
     * concurrent bookings (even from different machines) are applied one after the other,
     * and the second one sees available_seats >= ? fail.
     */
    private void reserveSeats(Connection con, Ticket t) throws SQLException, SeatNotAvailableException {
        int count = t.getPassengerCount();

        try (PreparedStatement ps = con.prepareStatement(
                "insert into seat_availability (train_number, seat_class, journey_date, available_seats) "
              + "select train_number, seat_class, cast(? as date), total_seats "
              + "  from train_classes where train_number = ? and seat_class = ? "
              + "on conflict do nothing")) {
            ps.setDate(1, java.sql.Date.valueOf(t.getJourneyDate()));
            ps.setInt(2, t.getTrainNumber());
            ps.setString(3, t.getSeatClass().getCode());
            ps.executeUpdate();
        }

        try (PreparedStatement ps = con.prepareStatement(
                "update seat_availability set available_seats = available_seats - ? "
              + " where train_number = ? and seat_class = ? and journey_date = ? "
              + "   and available_seats >= ?")) {
            ps.setInt(1, count);
            ps.setInt(2, t.getTrainNumber());
            ps.setString(3, t.getSeatClass().getCode());
            ps.setDate(4, java.sql.Date.valueOf(t.getJourneyDate()));
            ps.setInt(5, count);
            if (ps.executeUpdate() == 0) {
                throw new SeatNotAvailableException("Not enough " + t.getSeatClass().getCode()
                        + " seats on train " + t.getTrainNumber() + " for " + t.getJourneyDate()
                        + " (" + count + " requested)");
            }
        }
    }

    private void insertTicket(Connection con, Ticket t) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "insert into tickets (pnr, user_id, train_number, seat_class, journey_date, "
              + "from_station, to_station, status, total_fare) values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, t.getPnr());
            ps.setLong(2, t.getUserId());
            ps.setInt(3, t.getTrainNumber());
            ps.setString(4, t.getSeatClass().getCode());
            ps.setDate(5, java.sql.Date.valueOf(t.getJourneyDate()));
            ps.setString(6, t.getFromStation());
            ps.setString(7, t.getToStation());
            ps.setString(8, t.getStatus().name());
            ps.setBigDecimal(9, t.getTotalFare());
            ps.executeUpdate();
        }
    }

    private void insertPassengers(Connection con, Ticket t) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "insert into passengers (pnr, full_name, age, gender, berth_preference, seat_label) "
              + "values (?, ?, ?, ?, ?, ?)")) {
            for (Passenger p : t.getPassengers()) {
                ps.setString(1, t.getPnr());
                ps.setString(2, p.getFullName());
                ps.setInt(3, p.getAge());
                ps.setString(4, p.getGender().getCode());
                ps.setString(5, p.getBerthPreference().name());
                ps.setString(6, p.getSeatLabel());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertPayment(Connection con, Payment p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "insert into payments (pnr, amount, method, status) values (?, ?, ?, ?)")) {
            ps.setString(1, p.getPnr());
            ps.setBigDecimal(2, p.getAmount());
            ps.setString(3, p.getMethod().name());
            ps.setString(4, p.getStatus().name());
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------- cancellation

    @Override
    public void saveCancellation(Ticket t) {
        if (!t.isCancelled() || t.getRefundAmount() == null || t.getCancelledAt() == null) {
            throw new IllegalArgumentException("Call Ticket.cancel(...) before saving the cancellation");
        }
        if (t.getPassengerCount() == 0) {
            throw new IllegalArgumentException("Load the ticket with its passengers (findByPnr) first");
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Only a CONFIRMED ticket holds seats, so only that can be cancelled here.
                try (PreparedStatement ps = con.prepareStatement(
                        "update tickets set status = 'CANCELLED', refund_amount = ?, cancelled_at = ? "
                      + " where pnr = ? and status = 'CONFIRMED'")) {
                    ps.setBigDecimal(1, t.getRefundAmount());
                    ps.setTimestamp(2, Timestamp.valueOf(t.getCancelledAt()));
                    ps.setString(3, t.getPnr());
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException(
                                "Ticket " + t.getPnr() + " does not exist or is not a CONFIRMED booking");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "update seat_availability set available_seats = available_seats + ? "
                      + " where train_number = ? and seat_class = ? and journey_date = ?")) {
                    ps.setInt(1, t.getPassengerCount());
                    ps.setInt(2, t.getTrainNumber());
                    ps.setString(3, t.getSeatClass().getCode());
                    ps.setDate(4, java.sql.Date.valueOf(t.getJourneyDate()));
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException("No seat inventory row to return the seats to");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "update payments set status = 'REFUNDED' where pnr = ? and status = 'SUCCESS'")) {
                    ps.setString(1, t.getPnr());
                    ps.executeUpdate();
                }

                con.commit();
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Cancellation failed for PNR " + t.getPnr(), e);
        }
    }

    // ------------------------------------------------------------------- mapping

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getString("pnr"),
                rs.getLong("user_id"),
                rs.getInt("train_number"),
                SeatClass.fromCode(rs.getString("seat_class")),
                rs.getObject("journey_date", LocalDate.class),
                rs.getString("from_station"),
                rs.getString("to_station"),
                BookingStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("total_fare"),
                rs.getBigDecimal("refund_amount"),
                toLocal(rs.getObject("booked_at", OffsetDateTime.class)),
                toLocal(rs.getObject("cancelled_at", OffsetDateTime.class)));
    }

    private void loadPassengers(Connection con, Ticket ticket) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "select passenger_id, pnr, full_name, age, gender, berth_preference, seat_label "
              + "  from passengers where pnr = ? order by passenger_id")) {
            ps.setString(1, ticket.getPnr());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String berth = rs.getString("berth_preference");
                    ticket.addPassenger(new Passenger(
                            rs.getLong("passenger_id"),
                            rs.getString("pnr"),
                            rs.getString("full_name"),
                            rs.getInt("age"),
                            Passenger.Gender.fromCode(rs.getString("gender")),
                            berth == null ? null : Passenger.BerthPreference.valueOf(berth),
                            rs.getString("seat_label")));
                }
            }
        }
    }

    private static LocalDateTime toLocal(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}