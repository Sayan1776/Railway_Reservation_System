package com.railway;

import com.railway.model.Passenger;
import com.railway.model.Payment;
import com.railway.model.SeatClass;
import com.railway.model.Ticket;
import com.railway.model.Train;
import com.railway.model.User;
import com.railway.repository.TicketRepository;
import com.railway.repository.TrainRepository;
import com.railway.repository.jdbc.JdbcTicketRepository;
import com.railway.repository.jdbc.JdbcTrainRepository;
import com.railway.service.BookingService;
import com.railway.service.CancellationService;
import com.railway.service.DistanceFareCalculator;
import com.railway.service.PaymentService;
import com.railway.service.TrainService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Throwaway end-to-end check of the service layer. Do not commit. */
public class ServicesCheck {

    interface Action { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        TrainRepository trainRepo = new JdbcTrainRepository();
        TicketRepository ticketRepo = new JdbcTicketRepository();
        TrainService trainService = new TrainService(trainRepo);
        BookingService booking = new BookingService(trainRepo, ticketRepo,
                new DistanceFareCalculator(), new PaymentService());
        CancellationService cancellation = new CancellationService(trainRepo, ticketRepo);

        User owner = new User(1L, "Test User", "test@example.com", "9876543210", "x", "x", null);
        User stranger = new User(2L, "Other User", "other@example.com", "9876543211", "x", "x", null);

        LocalDate monday = LocalDate.of(2026, 10, 12);
        LocalDate tuesday = LocalDate.of(2026, 10, 13);

        List<Train> found = trainService.search("hwh", "gaya", monday);
        System.out.println("1. search HWH->GAYA " + monday + ": " + found.size() + " train(s) (expect 2)");

        Ticket t = booking.book(1, 10001, "HWH", "GAYA", monday, SeatClass.AC3,
                passengers(2), Payment.Method.UPI);
        System.out.println("2. " + t + " | fare " + t.getTotalFare() + " (expect 1282.40) | 3A free: "
                + trainRepo.getAvailableSeats(10001, SeatClass.AC3, monday) + " (expect 118)");

        blocked("3a. train does not run that day", () ->
                booking.book(1, 10002, "HWH", "GAYA", tuesday, SeatClass.AC3, passengers(1), Payment.Method.UPI));
        blocked("3b. 7 passengers", () ->
                booking.book(1, 10001, "HWH", "GAYA", monday, SeatClass.AC3, passengers(7), Payment.Method.UPI));
        blocked("3c. date in the past", () ->
                booking.book(1, 10001, "HWH", "GAYA", LocalDate.of(2026, 10, 1), SeatClass.AC3, passengers(1), Payment.Method.UPI));
        blocked("3d. unknown train", () ->
                booking.book(1, 99999, "HWH", "GAYA", monday, SeatClass.AC3, passengers(1), Payment.Method.UPI));
        blocked("3e. wrong direction", () ->
                booking.book(1, 10001, "GAYA", "HWH", monday, SeatClass.AC3, passengers(1), Payment.Method.UPI));
        blocked("3f. class the train lacks", () ->
                booking.book(1, 10002, "HWH", "GAYA", monday, SeatClass.AC1, passengers(1), Payment.Method.UPI));

        blocked("4. another user cannot cancel it", () -> cancellation.cancel(stranger, t.getPnr()));

        Ticket cancelled = cancellation.cancel(owner, t.getPnr());
        System.out.println("5. " + cancelled.getStatus() + ", refund " + cancelled.getRefundAmount()
                + " (expect 1154.16) | 3A free: "
                + trainRepo.getAvailableSeats(10001, SeatClass.AC3, monday) + " (expect 120)");

        blocked("6. cancelling twice", () -> cancellation.cancel(owner, t.getPnr()));
    }

    static List<Passenger> passengers(int n) {
        List<Passenger> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(new Passenger("Passenger " + i, 20 + i, Passenger.Gender.FEMALE, Passenger.BerthPreference.NONE));
        }
        return list;
    }

    static void blocked(String label, Action action) {
        try {
            action.run();
            System.out.println(label + ": FAIL, no exception thrown");
        } catch (Exception e) {
            System.out.println(label + ": blocked (" + e.getClass().getSimpleName() + ") " + e.getMessage());
        }
    }
}