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

import static com.railway.ui.ConsoleHelper.*;

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
        console.heading("Admin Panel: " + admin.getFullName());
        boolean running = true;
        while (running) {
            int choice = console.showMenu("ADMIN MENU", new String[]{
                    YELLOW + "1" + RESET + ". List all trains",
                    YELLOW + "2" + RESET + ". List stations",
                    YELLOW + "3" + RESET + ". Look up ticket by PNR",
                    YELLOW + "4" + RESET + ". Cancel any ticket",
                    YELLOW + "0" + RESET + ". " + DIM + "Logout" + RESET
            }, 0, 4);
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
        console.info("Logged out.");
    }

    private void listTrains() {
        console.heading("All Trains");
        for (Train t : trainService.getAllTrains()) {
            System.out.println();
            System.out.println(BOLD + CYAN + "  " + t.getTrainNumber() + RESET + "  "
                    + BOLD + t.getName() + RESET + "  " + DIM + "runs: " + days(t.getRunsOn()) + RESET);
            if (t.getRoute() == null) {
                System.out.println(YELLOW + "    (no route defined)" + RESET);
            } else {
                console.thinLine(55);
                for (RouteStop s : t.getRoute().getStops()) {
                    String arr = DateUtil.format(s.getArrivalTime());
                    String dep = DateUtil.format(s.getDepartureTime());
                    System.out.printf("    " + DIM + "%2d." + RESET + " " + BOLD + "%-5s" + RESET
                                    + " arr " + GREEN + "%s" + RESET + "  dep " + GREEN + "%s" + RESET
                                    + "  " + DIM + "%4d km" + RESET + "%n",
                            s.getStopOrder(), s.getStation().getCode(), arr, dep, s.getDistanceKm());
                }
                console.thinLine(55);
            }
            StringBuilder classes = new StringBuilder();
            for (TrainClass tc : t.getClasses()) {
                classes.append(MAGENTA + tc.getSeatClass().getCode() + RESET + " x" + tc.getTotalSeats()
                        + " @" + tc.getFarePerKm() + "/km   ");
            }
            System.out.println("    Classes: " + classes.toString().trim());
        }
    }

    private void listStations() {
        console.heading("Stations");
        System.out.println();
        System.out.printf("    " + BOLD + "%-6s %-30s %s" + RESET + "%n", "CODE", "NAME", "CITY");
        console.thinLine(55);
        for (Station s : trainService.getStations()) {
            System.out.printf("    " + CYAN + "%-6s" + RESET + " %-30s " + DIM + "%s" + RESET + "%n",
                    s.getCode(), s.getName(), s.getCity());
        }
    }

    private void lookUp() throws InvalidPNRException {
        console.printTicket(ticketService.find(admin, console.readLine("PNR: ")));
    }

    private void cancel() throws InvalidPNRException, InvalidInputException {
        String pnr = console.readLine("PNR to cancel: ");
        console.printTicket(ticketService.find(admin, pnr));
        if (!console.confirm("Cancel this ticket")) {
            console.info("Nothing was cancelled.");
            return;
        }
        console.showLoading("Processing cancellation...");
        Ticket cancelled = cancellationService.cancel(admin, pnr);
        console.success("Cancelled. Refund: Rs. " + cancelled.getRefundAmount());
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