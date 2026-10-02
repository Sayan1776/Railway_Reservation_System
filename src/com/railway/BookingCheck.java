package com.railway;

import com.railway.exception.SeatNotAvailableException;
import com.railway.model.BookingStatus;
import com.railway.model.Passenger;
import com.railway.model.Payment;
import com.railway.model.SeatClass;
import com.railway.model.Ticket;
import com.railway.repository.TicketRepository;
import com.railway.repository.TrainRepository;
import com.railway.repository.jdbc.JdbcTicketRepository;
import com.railway.repository.jdbc.JdbcTrainRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Throwaway check of booking, overbooking protection and cancellation. Do not commit. */
public class BookingCheck {

    static final long USER_ID = 1;   // user_id of the test user inserted in Supabase

    public static void main(String[] args) throws Exception {
        TrainRepository trains = new JdbcTrainRepository();
        TicketRepository tickets = new JdbcTicketRepository();
        LocalDate d1 = LocalDate.of(2026, 10, 5);
        LocalDate d2 = LocalDate.of(2026, 10, 6);

        // 1. a normal booking: 2 passengers, 3A, 458 km * 1.40 * 2 = 1282.40
        Ticket t1 = ticket("1000000001", SeatClass.AC3, d1, "1282.40", 2);
        tickets.book(t1, payment(t1));
        System.out.println("1. booked, 3A free: "
                + trains.getAvailableSeats(10001, SeatClass.AC3, d1) + " (expect 118)");

        // 2. fill every 1A seat on the second date
        Ticket full = ticket("1000000002", SeatClass.AC1, d2, "100.00", 18);
        tickets.book(full, payment(full));
        System.out.println("2. 1A free on " + d2 + ": "
                + trains.getAvailableSeats(10001, SeatClass.AC1, d2) + " (expect 0)");

        // 3. one more passenger must be refused and leave NOTHING behind
        Ticket extra = ticket("1000000003", SeatClass.AC1, d2, "100.00", 1);
        try {
            tickets.book(extra, payment(extra));
            System.out.println("3. FAIL: overbooking was allowed");
        } catch (SeatNotAvailableException e) {
            System.out.println("3. overbooking blocked: " + e.getMessage());
        }
        System.out.println("   rolled back, extra ticket exists: "
                + tickets.findByPnr("1000000003").isPresent() + " (expect false)");

        // 4. read it back
        Ticket loaded = tickets.findByPnr("1000000001").orElseThrow();
        System.out.println("4. " + loaded + " | passengers: " + loaded.getPassengerCount()
                + " | fare " + loaded.getTotalFare());
        System.out.println("   user's tickets: " + tickets.findByUser(USER_ID).size() + " (expect 2)");

        // 5. cancel it: seats come back, refund is stored
        loaded.cancel(new BigDecimal("1000.00"), LocalDateTime.now());
        tickets.saveCancellation(loaded);
        Ticket after = tickets.findByPnr("1000000001").orElseThrow();
        System.out.println("5. " + after.getStatus() + ", refund " + after.getRefundAmount()
                + ", 3A free: " + trains.getAvailableSeats(10001, SeatClass.AC3, d1) + " (expect 120)");

        // 6. cancelling twice must fail
        try {
            tickets.saveCancellation(after);
            System.out.println("6. FAIL: second cancellation was accepted");
        } catch (IllegalStateException e) {
            System.out.println("6. second cancel blocked: " + e.getMessage());
        }
    }

    static Ticket ticket(String pnr, SeatClass seatClass, LocalDate date, String fare, int passengers) {
        Ticket t = new Ticket(pnr, USER_ID, 10001, seatClass, date, "HWH", "GAYA",
                BookingStatus.CONFIRMED, new BigDecimal(fare), null, null, null);
        for (int i = 1; i <= passengers; i++) {
            t.addPassenger(new Passenger("Passenger " + i, 20 + i,
                    Passenger.Gender.MALE, Passenger.BerthPreference.LOWER));
        }
        return t;
    }

    static Payment payment(Ticket t) {
        return new Payment(null, t.getPnr(), t.getTotalFare(),
                Payment.Method.UPI, Payment.Status.SUCCESS, null);
    }
}