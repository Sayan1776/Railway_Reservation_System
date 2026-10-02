package com.railway.ui;

import com.railway.exception.InvalidInputException;
import com.railway.exception.InvalidPNRException;
import com.railway.exception.SeatNotAvailableException;
import com.railway.exception.TrainNotFoundException;
import com.railway.model.Passenger;
import com.railway.model.Payment;
import com.railway.model.Person;
import com.railway.model.RouteStop;
import com.railway.model.SeatClass;
import com.railway.model.Station;
import com.railway.model.Ticket;
import com.railway.model.Train;
import com.railway.model.TrainClass;
import com.railway.service.BookingService;
import com.railway.service.CancellationService;
import com.railway.service.FareCalculator;
import com.railway.service.TicketService;
import com.railway.service.TrainService;
import com.railway.util.DateUtil;
import com.railway.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserMenu {

    private final Person user;
    private final ConsoleHelper console;
    private final TrainService trainService;
    private final BookingService bookingService;
    private final CancellationService cancellationService;
    private final TicketService ticketService;
    private final FareCalculator fareCalculator;

    public UserMenu(Person user, ConsoleHelper console, TrainService trainService,
                    BookingService bookingService, CancellationService cancellationService,
                    TicketService ticketService, FareCalculator fareCalculator) {
        this.user = user;
        this.console = console;
        this.trainService = trainService;
        this.bookingService = bookingService;
        this.cancellationService = cancellationService;
        this.ticketService = ticketService;
        this.fareCalculator = fareCalculator;
    }

    public void run() {
        console.heading("Welcome, " + user.getFullName());
        boolean running = true;
        while (running) {
            System.out.println("\n1. Search trains");
            System.out.println("2. Book a ticket");
            System.out.println("3. My bookings");
            System.out.println("4. Check a ticket by PNR");
            System.out.println("5. Cancel a ticket");
            System.out.println("0. Logout");
            int choice = console.readInt("Choose: ", 0, 5);
            try {
                switch (choice) {
                    case 1: search(); break;
                    case 2: book(); break;
                    case 3: myBookings(); break;
                    case 4: checkPnr(); break;
                    case 5: cancel(); break;
                    default: running = false; break;
                }
            } catch (Exception e) {
                console.error(e);
            }
        }
    }

    // ----------------------------------------------------------------- search

    private void search() throws InvalidInputException {
        showStations();
        String from = console.readLine("From station code: ").toUpperCase();
        String to = console.readLine("To station code: ").toUpperCase();
        LocalDate date = console.readDate("Journey date");

        List<Train> trains = trainService.search(from, to, date);
        if (trains.isEmpty()) {
            System.out.println("  No trains run between " + from + " and " + to + " on " + DateUtil.format(date));
            return;
        }
        for (Train train : trains) {
            System.out.printf("%n%d  %s%n   %s%n", train.getTrainNumber(), train.getName(), timing(train, from, to));
            Map<SeatClass, Integer> free = trainService.getAvailability(train, date);
            for (TrainClass tc : train.getClasses()) {
                System.out.printf("   %-3s %-15s free %3d   Rs. %s%n",
                        tc.getSeatClass().getCode(), tc.getSeatClass().getDisplayName(),
                        free.get(tc.getSeatClass()),
                        fareCalculator.calculate(train, tc.getSeatClass(), from, to, 1));
            }
        }
    }

    // ------------------------------------------------------------------- book

    private void book() throws InvalidInputException, TrainNotFoundException, SeatNotAvailableException {
        showStations();
        String from = console.readLine("From station code: ").toUpperCase();
        String to = console.readLine("To station code: ").toUpperCase();
        LocalDate date = console.readDate("Journey date");

        List<Train> trains = trainService.search(from, to, date);
        if (trains.isEmpty()) {
            System.out.println("  No trains run between " + from + " and " + to + " on " + DateUtil.format(date));
            return;
        }

        List<String> trainOptions = new ArrayList<>();
        for (Train t : trains) {
            trainOptions.add(t.getTrainNumber() + "  " + t.getName() + "  " + timing(t, from, to));
        }
        int trainIndex = console.choose("Select a train", trainOptions, true);
        if (trainIndex < 0) {
            return;
        }
        Train train = trains.get(trainIndex);

        Map<SeatClass, Integer> free = trainService.getAvailability(train, date);
        List<TrainClass> classes = new ArrayList<>(train.getClasses());
        List<String> classOptions = new ArrayList<>();
        for (TrainClass tc : classes) {
            classOptions.add(String.format("%-3s %-15s free %3d   Rs. %s per passenger",
                    tc.getSeatClass().getCode(), tc.getSeatClass().getDisplayName(),
                    free.get(tc.getSeatClass()),
                    fareCalculator.calculate(train, tc.getSeatClass(), from, to, 1)));
        }
        int classIndex = console.choose("Select a class", classOptions, true);
        if (classIndex < 0) {
            return;
        }
        SeatClass seatClass = classes.get(classIndex).getSeatClass();
        int available = free.get(seatClass);
        if (available == 0) {
            System.out.println("  No seats left in " + seatClass.getCode() + " on that date.");
            return;
        }

        int max = Math.min(BookingService.MAX_PASSENGERS, available);
        int count = console.readInt("Number of passengers (1-" + max + "): ", 1, max);
        List<Passenger> passengers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            System.out.println("\nPassenger " + i);
            String name = console.read("  Name: ", Validator::requireName);
            int age = console.readInt("  Age (1-120): ", 1, 120);
            Passenger.Gender gender = console.chooseEnum("  Gender", Passenger.Gender.values());
            Passenger.BerthPreference berth =
                    console.chooseEnum("  Berth preference", Passenger.BerthPreference.values());
            passengers.add(new Passenger(name, age, gender, berth));
        }

        Payment.Method method = console.chooseEnum("Payment method", Payment.Method.values());
        BigDecimal fare = fareCalculator.calculate(train, seatClass, from, to, count);
        System.out.println("\nTotal fare for " + count + " passenger(s): Rs. " + fare);
        if (!console.confirm("Pay and book")) {
            System.out.println("  Booking cancelled.");
            return;
        }

        Ticket ticket = bookingService.book(user.getId(), train.getTrainNumber(), from, to,
                date, seatClass, passengers, method);
        console.heading("Booking confirmed");
        console.printTicket(ticket);
    }

    // ----------------------------------------------------- bookings, PNR, cancel

    private void myBookings() {
        List<Ticket> tickets = ticketService.forUser(user);
        if (tickets.isEmpty()) {
            System.out.println("  You have no bookings yet.");
            return;
        }
        System.out.println();
        for (Ticket t : tickets) {
            System.out.printf("  %s | train %d | %s -> %s | %s | %s | %s | Rs. %s%n",
                    t.getPnr(), t.getTrainNumber(), t.getFromStation(), t.getToStation(),
                    DateUtil.format(t.getJourneyDate()), t.getSeatClass().getCode(),
                    t.getStatus(), t.getTotalFare());
        }
    }

    private void checkPnr() throws InvalidPNRException {
        console.printTicket(ticketService.find(user, console.readLine("PNR: ")));
    }

    private void cancel() throws InvalidPNRException, InvalidInputException {
        String pnr = console.readLine("PNR to cancel: ");
        console.printTicket(ticketService.find(user, pnr));
        System.out.println("\n  The refund depends on how much time is left before departure.");
        if (!console.confirm("Cancel this ticket")) {
            System.out.println("  Nothing was cancelled.");
            return;
        }
        Ticket cancelled = cancellationService.cancel(user, pnr);
        console.heading("Ticket cancelled");
        System.out.println("  Refund: Rs. " + cancelled.getRefundAmount());
    }

    // ---------------------------------------------------------------- helpers

    private void showStations() {
        System.out.println("\nStations:");
        for (Station s : trainService.getStations()) {
            System.out.printf("  %-5s %s, %s%n", s.getCode(), s.getName(), s.getCity());
        }
        System.out.println();
    }

    /** "HWH 16:50 -> GAYA 23:10 | 458 km" with a (+1) marker when it arrives on a later day. */
    private String timing(Train train, String from, String to) {
        RouteStop a = train.getRoute().findStop(from).get();
        RouteStop b = train.getRoute().findStop(to).get();
        int daysLater = b.getDayOffset() - a.getDayOffset();
        return from + " " + DateUtil.format(a.getDepartureTime()) + " -> "
                + to + " " + DateUtil.format(b.getArrivalTime())
                + (daysLater > 0 ? " (+" + daysLater + ")" : "")
                + " | " + train.getRoute().distanceBetween(from, to) + " km";
    }
}