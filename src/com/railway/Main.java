package com.railway;

import com.railway.exception.AuthenticationException;
import com.railway.exception.DataAccessException;
import com.railway.exception.InvalidInputException;
import com.railway.model.Person;
import com.railway.repository.TicketRepository;
import com.railway.repository.TrainRepository;
import com.railway.repository.UserRepository;
import com.railway.repository.jdbc.JdbcTicketRepository;
import com.railway.repository.jdbc.JdbcTrainRepository;
import com.railway.repository.jdbc.JdbcUserRepository;
import com.railway.service.AuthService;
import com.railway.service.BookingService;
import com.railway.service.CancellationService;
import com.railway.service.DistanceFareCalculator;
import com.railway.service.FareCalculator;
import com.railway.service.PaymentService;
import com.railway.service.TicketService;
import com.railway.service.TrainService;
import com.railway.ui.AdminMenu;
import com.railway.ui.ConsoleHelper;
import com.railway.ui.UserMenu;
import com.railway.util.Validator;

import static com.railway.ui.ConsoleHelper.*;

/** Wires everything together and runs the login loop. All logic lives in the services. */
public class Main {

    public static void main(String[] args) {
        ConsoleHelper console = new ConsoleHelper();
        console.printBanner();
        try {
            start(console);
        } catch (ConsoleHelper.InputClosedException e) {
            System.out.println("\n" + ConsoleHelper.MARGIN + DIM + "Input closed. Goodbye." + RESET);
        } catch (DataAccessException | ExceptionInInitializerError e) {
            System.out.println(RED + "\n" + ConsoleHelper.MARGIN +  "✖" + " Could not reach the database: " + e.getMessage() + RESET);
            if (e.getCause() != null) {
                System.out.println(DIM + ConsoleHelper.MARGIN + "Reason: " + e.getCause().getMessage() + RESET);
            }
            System.out.println(YELLOW + ConsoleHelper.MARGIN + "Check db.properties, your internet connection and that the Supabase project is running." + RESET);
        }
    }

    private static void start(ConsoleHelper console) {
        console.showLoading("Connecting to database...");
        TrainRepository trainRepo = new JdbcTrainRepository();
        TicketRepository ticketRepo = new JdbcTicketRepository();
        UserRepository userRepo = new JdbcUserRepository();
        console.showLoading("Initializing services...");

        FareCalculator fareCalculator = new DistanceFareCalculator();
        AuthService auth = new AuthService(userRepo);
        TrainService trainService = new TrainService(trainRepo);
        TicketService ticketService = new TicketService(ticketRepo);
        BookingService booking = new BookingService(trainRepo, ticketRepo, fareCalculator, new PaymentService());
        CancellationService cancellation = new CancellationService(trainRepo, ticketRepo);

        if (auth.needsAdminSetup()) {
            firstRunSetup(console, auth);
        }

        boolean running = true;
        while (running) {
            int choice = console.showMenu("MAIN MENU", new String[]{
                    YELLOW + "1" + RESET + ". Login",
                    YELLOW + "2" + RESET + ". Register",
                    YELLOW + "0" + RESET + ". " + DIM + "Exit" + RESET
            }, 0, 2);
            try {
                switch (choice) {
                    case 1:
                        Person person = login(console, auth);
                        if (person == null) {
                            break;
                        }
                        if (person.isAdmin()) {
                            new AdminMenu(person, console, trainService, ticketService, cancellation).run();
                        } else {
                            new UserMenu(person, console, trainService, booking, cancellation,
                                    ticketService, fareCalculator).run();
                        }
                        break;
                    case 2:
                        register(console, auth);
                        break;
                    default:
                        running = false;
                        break;
                }
            } catch (Exception e) {
                console.error(e);
            }
        }
        console.printGoodbye();
    }

    private static void firstRunSetup(ConsoleHelper console, AuthService auth) {
        console.heading("First Run: Create Admin Account");
        while (true) {
            String name = console.read("Admin name: ", Validator::requireName);
            String email = console.read("Admin email: ", Validator::requireEmail);
            String phone = console.read("Admin phone: ", Validator::requirePhone);
            String password = readNewPassword(console);
            try {
                auth.createFirstAdmin(name, email, phone, password);
                console.success("Admin account created. You can log in now.");
                return;
            } catch (InvalidInputException e) {
                console.error(e);
            }
        }
    }

    private static void register(ConsoleHelper console, AuthService auth) throws InvalidInputException {
        console.heading("Register New Account");
        String name = console.read("Full name: ", Validator::requireName);
        String email = console.read("Email: ", Validator::requireEmail);
        String phone = console.read("Mobile number: ", Validator::requirePhone);
        String password = readNewPassword(console);
        auth.register(name, email, phone, password);
        console.success("Account created. You can log in now.");
    }

    private static Person login(ConsoleHelper console, AuthService auth) {
        console.heading("Login");
        String email = console.readLine("Email: ");
        String password = console.readSecret("Password: ");
        console.showLoading("Authenticating...");
        try {
            Person person = auth.login(email, password);
            console.success("Welcome back, " + person.getFullName() + "!");
            return person;
        } catch (AuthenticationException e) {
            console.error(e);
            return null;
        }
    }

    private static String readNewPassword(ConsoleHelper console) {
        while (true) {
            String password = console.read("Password (8+ chars, letter and digit): ", text -> {
                Validator.requirePassword(text);
                return text;
            });
            if (password.equals(console.readSecret("Confirm password: "))) {
                return password;
            }
            System.out.println(RED + ConsoleHelper.MARGIN + "✖" + " Passwords do not match" + RESET);
        }
    }
}
