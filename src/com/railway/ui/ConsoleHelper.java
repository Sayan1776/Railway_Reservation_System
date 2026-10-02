package com.railway.ui;

import com.railway.exception.DataAccessException;
import com.railway.exception.InvalidInputException;
import com.railway.model.Passenger;
import com.railway.model.Ticket;
import com.railway.util.DateUtil;
import com.railway.util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** The only class that reads from the keyboard. Menus never touch Scanner directly. */
public class ConsoleHelper {

    public interface Parser<T> {
        T parse(String text) throws InvalidInputException;
    }

    /** Thrown when standard input ends, so the program can exit cleanly. */
    public static class InputClosedException extends RuntimeException {
        public InputClosedException() {
            super("Input closed");
        }
    }

    private final Scanner in = new Scanner(System.in);

    // ------------------------------------------------------------------ input

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            throw new InputClosedException();
        }
        return in.nextLine().trim();
    }

    /** Keeps asking until the parser accepts the text. */
    public <T> T read(String prompt, Parser<T> parser) {
        while (true) {
            try {
                return parser.parse(readLine(prompt));
            } catch (InvalidInputException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    public int readInt(String prompt, int min, int max) {
        return read(prompt, text -> Validator.requireInt(text, min, max, "Value"));
    }

    public LocalDate readDate(String label) {
        return read(label + " (dd-MM-yyyy): ", DateUtil::parse);
    }

    /** Note: Scanner cannot hide typing, so the password is visible in the console. */
    public String readSecret(String prompt) {
        return readLine(prompt);
    }

    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (y/n): ").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("  ! Please answer y or n");
        }
    }

    /** Shows a numbered list. Returns the 0-based index, or -1 if "Back" was chosen. */
    public int choose(String title, List<String> options, boolean allowBack) {
        System.out.println("\n" + title);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, options.get(i));
        }
        if (allowBack) {
            System.out.println("  0. Back");
        }
        return readInt("Choose: ", allowBack ? 0 : 1, options.size()) - 1;
    }

    public <E extends Enum<E>> E chooseEnum(String title, E[] values) {
        List<String> names = new ArrayList<>();
        for (E value : values) {
            names.add(pretty(value));
        }
        return values[choose(title, names, false)];
    }

    // ----------------------------------------------------------------- output

    public void heading(String text) {
        System.out.println("\n=== " + text + " ===");
    }

    /** Prints a friendly message. Input-closed is rethrown so the program can stop. */
    public void error(Exception e) {
        if (e instanceof InputClosedException) {
            throw (InputClosedException) e;
        }
        if (e instanceof DataAccessException) {
            Throwable cause = e.getCause();
            System.out.println("  ! Database problem: " + e.getMessage()
                    + (cause == null ? "" : " (" + cause.getMessage() + ")"));
        } else {
            System.out.println("  ! " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    public void printTicket(Ticket t) {
        System.out.println();
        System.out.println("  PNR        : " + t.getPnr());
        System.out.println("  Train      : " + t.getTrainNumber());
        System.out.println("  Journey    : " + t.getFromStation() + " -> " + t.getToStation()
                + " on " + DateUtil.format(t.getJourneyDate()));
        System.out.println("  Class      : " + t.getSeatClass());
        System.out.println("  Status     : " + t.getStatus());
        System.out.println("  Fare       : Rs. " + t.getTotalFare());
        if (t.isCancelled()) {
            System.out.println("  Refund     : Rs. " + t.getRefundAmount()
                    + " (cancelled " + DateUtil.formatDateTime(t.getCancelledAt()) + ")");
        }
        System.out.println("  Passengers :");
        int number = 1;
        for (Passenger p : t.getPassengers()) {
            System.out.println("    " + number++ + ". " + p + " [berth: " + pretty(p.getBerthPreference()) + "]");
        }
    }

    /** SIDE_LOWER becomes "Side lower". */
    public static String pretty(Enum<?> value) {
        String s = value.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}