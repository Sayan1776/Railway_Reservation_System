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

import static com.railway.ui.ConsoleHelper.*;

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
            int choice = console.showMenu("USER MENU", new String[]{
                    YELLOW + "1" + RESET + ". Search trains",
                    YELLOW + "2" + RESET + ". Book a ticket",
                    YELLOW + "3" + RESET + ". My bookings",
                    YELLOW + "4" + RESET + ". Check ticket by PNR",
                    YELLOW + "5" + RESET + ". Cancel a ticket",
                    YELLOW + "0" + RESET + ". " + DIM + "Logout" + RESET
            }, 0, 5);
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
        console.info("Logged out.");
    }

    // ----------------------------------------------------------------- search

    private void search() throws InvalidInputException {
        console.heading("Search Trains");
        showStations();
        String from = console.readLine("From station code: ").toUpperCase();
        String to = console.readLine("To station code: ").toUpperCase();
        LocalDate date = console.readDate("Journey date");

        console.showLoading("Searching trains...");
        List<Train> trains = trainService.search(from, to, date);
        if (trains.isEmpty()) {
            console.warn("No trains run between " + from + " and " + to + " on " + DateUtil.format(date));
            return;
        }
        for (Train train : trains) {
            System.out.println();
            System.out.println(BOLD + CYAN + "  " + train.getTrainNumber() + RESET + "  "
                    + BOLD + train.getName() + RESET);
            System.out.println(DIM + "    " + timing(train, from, to) + RESET);
            console.thinLine(55);
            Map<SeatClass, Integer> free = trainService.getAvailability(train, date);
            System.out.printf("    " + BOLD + "%-5s %-17s %6s   %s" + RESET + "%n", "CLASS", "TYPE", "AVAIL", "FARE (per person)");
            for (TrainClass tc : train.getClasses()) {
                int avail = free.get(tc.getSeatClass());
                String availColor = avail > 10 ? GREEN : (avail > 0 ? YELLOW : RED);
                System.out.printf("    " + MAGENTA + "%-5s" + RESET + " %-17s " + availColor + "%4d" + RESET
                                + "   Rs. " + GREEN + "%s" + RESET + "%n",
                        tc.getSeatClass().getCode(), tc.getSeatClass().getDisplayName(),
                        avail,
                        fareCalculator.calculate(train, tc.getSeatClass(), from, to, 1));
            }
        }
    }

    // ------------------------------------------------------------------- book

    private void book() throws InvalidInputException, TrainNotFoundException, SeatNotAvailableException {
        console.heading("Book a Ticket");
        showStations();
        String from = console.readLine("From station code: ").toUpperCase();
        String to = console.readLine("To station code: ").toUpperCase();
        LocalDate date = console.readDate("Journey date");

        console.showLoading("Searching trains...");
        List<Train> trains = trainService.search(from, to, date);
        if (trains.isEmpty()) {
            console.warn("No trains run between " + from + " and " + to + " on " + DateUtil.format(date));
            return;
        }

        List<String> trainOptions = new ArrayList<>();
        for (Train t : trains) {
            trainOptions.add(CYAN + t.getTrainNumber() + RESET + "  " + t.getName() + "  " + DIM + timing(t, from, to) + RESET);
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
            int avail = free.get(tc.getSeatClass());
            String availColor = avail > 10 ? GREEN : (avail > 0 ? YELLOW : RED);
            classOptions.add(String.format(MAGENTA + "%-3s" + RESET + " %-15s free " + availColor + "%3d" + RESET + "   Rs. " + GREEN + "%s" + RESET + " per person",
                    tc.getSeatClass().getCode(), tc.getSeatClass().getDisplayName(),
                    avail,
                    fareCalculator.calculate(train, tc.getSeatClass(), from, to, 1)));
        }
        int classIndex = console.choose("Select a class", classOptions, true);
        if (classIndex < 0) {
            return;
        }
        SeatClass seatClass = classes.get(classIndex).getSeatClass();
        int available = free.get(seatClass);
        if (available == 0) {
            console.warn("No seats left in " + seatClass.getCode() + " on that date.");
            return;
        }

        int max = Math.min(BookingService.MAX_PASSENGERS, available);
        int count = console.readInt("Number of passengers (1-" + max + "): ", 1, max);
        List<Passenger> passengers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            console.info("Passenger " + i + " of " + count);
            String name = console.read("  Name: ", Validator::requireName);
            int age = console.readInt("  Age (1-120): ", 1, 120);
            Passenger.Gender gender = console.chooseEnum("  Gender", Passenger.Gender.values());
            Passenger.BerthPreference berth =
                    console.chooseEnum("  Berth preference", Passenger.BerthPreference.values());
            passengers.add(new Passenger(name, age, gender, berth));
        }

        Payment.Method method = console.chooseEnum("Payment method", Payment.Method.values());
        BigDecimal fare = fareCalculator.calculate(train, seatClass, from, to, count);

        System.out.println();
        System.out.println(BOLD + "  Total fare for " + count + " passenger(s): " + GREEN + "Rs. " + fare + RESET);
        if (!console.confirm("Pay and book")) {
            console.info("Booking cancelled.");
            return;
        }

        console.showProgress("Processing payment", 800);
        console.showLoading("Confirming reservation...");
        Ticket ticket = bookingService.book(user.getId(), train.getTrainNumber(), from, to,
                date, seatClass, passengers, method);
        console.heading("Booking Confirmed!");
        console.printTicket(ticket);
    }

    // ----------------------------------------------------- bookings, PNR, cancel

    private void myBookings() {
        console.heading("My Bookings");
        List<Ticket> tickets = ticketService.forUser(user);
        if (tickets.isEmpty()) {
            console.info("You have no bookings yet.");
            return;
        }
        System.out.println();
        System.out.printf("  " + BOLD + "%-12s %-7s %-6s %-6s %-12s %-5s %-11s %s" + RESET + "%n",
                "PNR", "TRAIN", "FROM", "TO", "DATE", "CLASS", "STATUS", "FARE");
        console.thinLine(72);
        for (Ticket t : tickets) {
            String statusColor = t.isCancelled() ? RED : GREEN;
            System.out.printf("  " + YELLOW + "%-12s" + RESET + " %-7d %-6s %-6s %-12s " + MAGENTA + "%-5s" + RESET + " "
                            + statusColor + "%-11s" + RESET + " Rs. " + GREEN + "%s" + RESET + "%n",
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
        console.warn("The refund depends on how much time is left before departure.");
        if (!console.confirm("Cancel this ticket")) {
            console.info("Nothing was cancelled.");
            return;
        }
        console.showLoading("Processing cancellation...");
        Ticket cancelled = cancellationService.cancel(user, pnr);
        console.heading("Ticket Cancelled");
        console.success("Refund: Rs. " + cancelled.getRefundAmount());
    }

    // ---------------------------------------------------------------- helpers

    private void showStations() {
        System.out.println();
        System.out.println(BOLD + "  Available Stations:" + RESET);
        console.thinLine(50);
        for (Station s : trainService.getStations()) {
            System.out.printf("    " + CYAN + "%-5s" + RESET + " %s, " + DIM + "%s" + RESET + "%n",
                    s.getCode(), s.getName(), s.getCity());
        }
        System.out.println();
    }

    /** "HWH 16:50 → GAYA 23:10 (+1) | 458 km" */
    private String timing(Train train, String from, String to) {
        RouteStop a = train.getRoute().findStop(from).get();
        RouteStop b = train.getRoute().findStop(to).get();
        int daysLater = b.getDayOffset() - a.getDayOffset();
        return from + " " + DateUtil.format(a.getDepartureTime()) + " → "
                + to + " " + DateUtil.format(b.getArrivalTime())
                + (daysLater > 0 ? " (+" + daysLater + ")" : "")
                + " | " + train.getRoute().distanceBetween(from, to) + " km";
    }
}