package com.railway.ui;

import com.railway.exception.InvalidInputException;
import com.railway.exception.InvalidPNRException;
import com.railway.model.Person;
import com.railway.model.RouteStop;
import com.railway.model.Station;
import com.railway.model.Ticket;
import com.railway.model.Train;
import com.railway.model.TrainClass;
import com.railway.service.CancellationService;
import com.railway.service.TicketService;
import com.railway.service.TrainService;
import com.railway.util.DateUtil;

public class AdminMenu {

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final Person admin;
    private final ConsoleHelper console;
    private final TrainService trainService;
    private final TicketService ticketService;
    private final CancellationService cancellationService;

    public AdminMenu(Person admin, ConsoleHelper console, TrainService trainService,
                     TicketService ticketService, CancellationService cancellationService) {
        this.admin = admin;
        this.console = console;
        this.trainService = trainService;
        this.ticketService = ticketService;
        this.cancellationService = cancellationService;
    }

    public void run() {
        console.heading("Admin: " + admin.getFullName());
        boolean running = true;
        while (running) {
            System.out.println("\n1. List all trains");
            System.out.println("2. List stations");
            System.out.println("3. Look up a ticket by PNR");
            System.out.println("4. Cancel any ticket");
            System.out.println("0. Logout");
            int choice = console.readInt("Choose: ", 0, 4);
            try {
                switch (choice) {
                    case 1: listTrains(); break;
                    case 2: listStations(); break;
                    case 3: lookUp(); break;
                    case 4: cancel(); break;
                    default: running = false; break;
                }
            } catch (Exception e) {
                console.error(e);
            }
        }
    }

    private void listTrains() {
        for (Train t : trainService.getAllTrains()) {
            System.out.printf("%n%d  %s  runs: %s%n", t.getTrainNumber(), t.getName(), days(t.getRunsOn()));
            if (t.getRoute() == null) {
                System.out.println("   (no route defined)");
            } else {
                for (RouteStop s : t.getRoute().getStops()) {
                    System.out.printf("   %2d. %-5s arr %s  dep %s  %4d km%n", s.getStopOrder(),
                            s.getStation().getCode(), DateUtil.format(s.getArrivalTime()),
                            DateUtil.format(s.getDepartureTime()), s.getDistanceKm());
                }
            }
            StringBuilder classes = new StringBuilder();
            for (TrainClass tc : t.getClasses()) {
                classes.append(tc).append("   ");
            }
            System.out.println("   classes: " + classes.toString().trim());
        }
    }

    private void listStations() {
        System.out.println();
        for (Station s : trainService.getStations()) {
            System.out.printf("  %-5s %s, %s%n", s.getCode(), s.getName(), s.getCity());
        }
    }

    private void lookUp() throws InvalidPNRException {
        console.printTicket(ticketService.find(admin, console.readLine("PNR: ")));
    }

    private void cancel() throws InvalidPNRException, InvalidInputException {
        String pnr = console.readLine("PNR to cancel: ");
        console.printTicket(ticketService.find(admin, pnr));
        if (!console.confirm("Cancel this ticket")) {
            System.out.println("  Nothing was cancelled.");
            return;
        }
        Ticket cancelled = cancellationService.cancel(admin, pnr);
        System.out.println("  Cancelled. Refund: Rs. " + cancelled.getRefundAmount());
    }

    private static String days(String runsOn) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runsOn.length(); i++) {
            if (runsOn.charAt(i) == 'Y') {
                sb.append(sb.length() == 0 ? "" : " ").append(DAYS[i]);
            }
        }
        return sb.toString();
    }
}