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

/**
 * The only class that reads from the keyboard. Menus never touch Scanner directly.
 * Provides a beautiful, polished console UI with box-drawing characters and ANSI colors.
 */
public class ConsoleHelper {

    // ─── ANSI color codes ───────────────────────────────────────────────
    public static final String RESET   = "\033[0m";
    public static final String BOLD    = "\033[1m";
    public static final String DIM     = "\033[2m";
    public static final String CYAN    = "\033[36m";
    public static final String GREEN   = "\033[32m";
    public static final String YELLOW  = "\033[33m";
    public static final String RED     = "\033[31m";
    public static final String MAGENTA = "\033[35m";
    public static final String BLUE    = "\033[34m";
    public static final String WHITE   = "\033[37m";
    public static final String BG_BLUE = "\033[44m";

    // ─── Alignment Margin ───────────────────────────────────────────────
    public static final String MARGIN  = "                        "; // 24 spaces for centering

    // ─── Box-drawing characters ─────────────────────────────────────────
    private static final String H_LINE  = "─";
    private static final String V_LINE  = "│";
    private static final String TL      = "╔";
    private static final String TR      = "╗";
    private static final String BL      = "╚";
    private static final String BR      = "╝";
    private static final String H_DOUBLE = "═";
    private static final String T_LEFT  = "╠";
    private static final String T_RIGHT = "╣";
    private static final String DOT     = "●";
    private static final String ARROW   = "▶";
    private static final String CHECK   = "✔";
    private static final String CROSS   = "✖";
    private static final String TRAIN   = "🚂";
    private static final String TICKET_ICON = "🎫";

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

    // ─────────────────────────────────────── Loading animation ───────────

    /** Displays a brief spinner animation with a message. */
    public void showLoading(String message) {
        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        try {
            for (int i = 0; i < 10; i++) {
                System.out.print("\r" + MARGIN + CYAN + frames[i % frames.length] + RESET + " " + message);
                Thread.sleep(80);
            }
            System.out.print("\r" + MARGIN + GREEN + CHECK + RESET + " " + message + "                \n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Displays a progress bar animation. */
    public void showProgress(String message, int durationMs) {
        int width = 30;
        int steps = 20;
        int delay = durationMs / steps;
        try {
            for (int i = 0; i <= steps; i++) {
                int filled = (i * width) / steps;
                int empty = width - filled;
                StringBuilder bar = new StringBuilder();
                bar.append(GREEN);
                for (int j = 0; j < filled; j++) bar.append("█");
                bar.append(DIM);
                for (int j = 0; j < empty; j++) bar.append("░");
                bar.append(RESET);
                int percent = (i * 100) / steps;
                System.out.printf("\r  %s [%s] %3d%%", message, bar, percent);
                Thread.sleep(delay);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Types out text character by character for dramatic effect. */
    public void typewrite(String text, int charDelayMs) {
        try {
            for (char c : text.toCharArray()) {
                System.out.print(c);
                Thread.sleep(charDelayMs);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(text);
        }
    }

    // ─────────────────────────────────────── Input methods ───────────────

    public String readLine(String prompt) {
        System.out.print(CYAN + MARGIN + ARROW + " " + RESET + prompt);
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
                System.out.println(RED + MARGIN + CROSS + " " + e.getMessage() + RESET);
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
            String answer = readLine(prompt + " (" + GREEN + "y" + RESET + "/" + RED + "n" + RESET + "): ").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println(RED + MARGIN + CROSS + " Please answer y or n" + RESET);
        }
    }

    /** Shows a numbered list. Returns the 0-based index, or -1 if "Back" was chosen. */
    public int choose(String title, List<String> options, boolean allowBack) {
        System.out.println();
        System.out.println(BOLD + CYAN + MARGIN + title + RESET);
        thinLine(50);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf(MARGIN + YELLOW + "%d" + RESET + ". %s%n", i + 1, options.get(i));
        }
        if (allowBack) {
            System.out.println(MARGIN + DIM + "0. Back" + RESET);
        }
        thinLine(50);
        return readInt("Choose: ", allowBack ? 0 : 1, options.size()) - 1;
    }

    public <E extends Enum<E>> E chooseEnum(String title, E[] values) {
        List<String> names = new ArrayList<>();
        for (E value : values) {
            names.add(pretty(value));
        }
        return values[choose(title, names, false)];
    }

    // ─────────────────────────────────────── Output methods ──────────────

    /** Draws a major section heading in a double-bordered box. */
    public void heading(String text) {
        int width = Math.max(text.length() + 4, 40);
        String pad = repeat(H_DOUBLE, width);
        System.out.println();
        System.out.println(BOLD + CYAN + MARGIN + TL + pad + TR + RESET);
        String inner = centerText(text, width);
        System.out.println(BOLD + CYAN + MARGIN + V_LINE + RESET + BOLD + inner + BOLD + CYAN + V_LINE + RESET);
        System.out.println(BOLD + CYAN + MARGIN + BL + pad + BR + RESET);
    }

    /** Draws a thin separator line. */
    public void thinLine(int width) {
        System.out.println(DIM + MARGIN + repeat(H_LINE, width) + RESET);
    }

    /** Prints a success message. */
    public void success(String message) {
        System.out.println(GREEN + MARGIN + CHECK + " " + message + RESET);
    }

    /** Prints an info message. */
    public void info(String message) {
        System.out.println(CYAN + MARGIN + DOT + " " + message + RESET);
    }

    /** Prints a warning message. */
    public void warn(String message) {
        System.out.println(YELLOW + MARGIN + "⚠ " + message + RESET);
    }

    /** Prints a friendly error message. Input-closed is rethrown so the program can stop. */
    public void error(Exception e) {
        if (e instanceof InputClosedException) {
            throw (InputClosedException) e;
        }
        if (e instanceof DataAccessException) {
            Throwable cause = e.getCause();
            System.out.println(RED + MARGIN + CROSS + " Database problem: " + e.getMessage()
                    + (cause == null ? "" : " (" + cause.getMessage() + ")") + RESET);
        } else {
            System.out.println(RED + MARGIN + CROSS + " " + (e.getMessage() != null ? e.getMessage() : e.toString()) + RESET);
        }
    }

    /** Prints a beautifully formatted ticket. */
    public void printTicket(Ticket t) {
        int w = 52;
        String border = repeat(H_DOUBLE, w);
        String thin   = repeat(H_LINE, w);
        System.out.println();
        System.out.println(BOLD + CYAN + MARGIN + TL + border + TR + RESET);
        System.out.println(BOLD + CYAN + MARGIN + V_LINE + RESET + centerText(TICKET_ICON + " RAILWAY TICKET " + TICKET_ICON, w) + BOLD + CYAN + V_LINE + RESET);
        System.out.println(BOLD + CYAN + MARGIN + T_LEFT + border + T_RIGHT + RESET);
        printField(w, "PNR",      BOLD + YELLOW + t.getPnr() + RESET);
        printField(w, "Train",    String.valueOf(t.getTrainNumber()));
        printField(w, "Journey",  t.getFromStation() + " → " + t.getToStation() + " on " + DateUtil.format(t.getJourneyDate()));
        printField(w, "Class",    t.getSeatClass().getDisplayName() + " (" + t.getSeatClass().getCode() + ")");
        String statusColor = t.isCancelled() ? RED : GREEN;
        printField(w, "Status",   statusColor + t.getStatus() + RESET);
        printField(w, "Fare",     GREEN + "Rs. " + t.getTotalFare() + RESET);
        if (t.isCancelled()) {
            printField(w, "Refund", YELLOW + "Rs. " + t.getRefundAmount() + RESET
                    + DIM + " (" + DateUtil.formatDateTime(t.getCancelledAt()) + ")" + RESET);
        }
        System.out.println(DIM + MARGIN + V_LINE + "  " + thin.substring(4) + "  " + V_LINE + RESET);
        System.out.println(BOLD + MARGIN + V_LINE + "  Passengers:" + padRight("", w - 14) + V_LINE + RESET);
        int number = 1;
        for (Passenger p : t.getPassengers()) {
            String pInfo = "  " + number++ + ". " + p + " [" + pretty(p.getBerthPreference()) + "]";
            System.out.println(MARGIN + V_LINE + padRight(pInfo, w) + V_LINE);
        }
        System.out.println(BOLD + CYAN + MARGIN + BL + border + BR + RESET);
    }

    private void printField(int boxWidth, String label, String value) {
        String content = "  " + DIM + label + RESET + padRight("", 10 - label.length()) + ": " + value;
        // We can't easily measure ANSI-colored string length, so just print and pad manually
        System.out.println(MARGIN + V_LINE + content + padForBox(content, boxWidth) + V_LINE);
    }

    // ─────────────────────────────────────── Banner ──────────────────────

    /** Prints the application startup banner with ASCII art. */
    public void printBanner() {
        String[] art = {
            "",
            BOLD + CYAN +  MARGIN + "╔══════════════════════════════════════════════════════╗" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + BOLD + WHITE +  "      ____       _ _                                  " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + BOLD + WHITE +  "     |  _ \\ __ _(_) |_      ____ _ _   _              " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + BOLD + WHITE +  "     | |_) / _` | | \\ \\ /\\ / / _` | | | |             " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + BOLD + WHITE +  "     |  _ < (_| | | |\\ V  V / (_| | |_| |             " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + BOLD + WHITE +  "     |_| \\_\\__,_|_|_| \\_/\\_/ \\__,_|\\__, |             " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + BOLD + WHITE +  "                                    |___/              " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + BOLD + GREEN +  "       R E S E R V A T I O N   S Y S T E M             " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + DIM +            "                                                       " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "║" + RESET + DIM +            "       " + TRAIN + "  Fast • Reliable • Convenient             " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN +  MARGIN + "╚══════════════════════════════════════════════════════╝" + RESET,
            ""
        };
        for (String line : art) {
            System.out.println(line);
        }
    }

    /** Prints a goodbye banner. */
    public void printGoodbye() {
        System.out.println();
        thinLine(50);
        typewrite(MARGIN + "Thank you for using Railway Reservation System!", 20);
        typewrite(MARGIN + "Have a safe journey! " + TRAIN, 20);
        thinLine(50);
        System.out.println();
    }

    // ─────────────────────────────────────── Menu box ────────────────────

    /** Prints a menu inside a bordered box. Returns the user's choice. */
    public int showMenu(String title, String[] options, int min, int max) {
        int width = 40;
        for (String opt : options) {
            width = Math.max(width, opt.length() + 10);
        }
        String border = repeat(H_DOUBLE, width);
        String thin   = repeat(H_LINE, width);

        System.out.println();
        System.out.println(BOLD + CYAN + MARGIN + TL + border + TR + RESET);
        System.out.println(BOLD + CYAN + MARGIN + V_LINE + RESET + centerText(title, width) + BOLD + CYAN + V_LINE + RESET);
        System.out.println(CYAN + MARGIN + T_LEFT + thin + T_RIGHT + RESET);
        for (String opt : options) {
            System.out.println(MARGIN + V_LINE + padRight("  " + opt, width) + V_LINE);
        }
        System.out.println(BOLD + CYAN + MARGIN + BL + border + BR + RESET);

        return readInt("Choose: ", min, max);
    }

    // ─────────────────────────────────────── Helpers ─────────────────────

    /** SIDE_LOWER becomes "Side lower". */
    public static String pretty(Enum<?> value) {
        String s = value.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }

    private static String centerText(String text, int width) {
        int textLen = stripAnsi(text).length();
        int padding = Math.max(0, width - textLen);
        int left = padding / 2;
        int right = padding - left;
        return padRight("", left) + text + padRight("", right);
    }

    private static String padRight(String text, int totalWidth) {
        int textLen = stripAnsi(text).length();
        int pad = Math.max(0, totalWidth - textLen);
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < pad; i++) sb.append(' ');
        return sb.toString();
    }

    /** Provides right-padding for box fields with ANSI codes. */
    private String padForBox(String ansiText, int boxWidth) {
        int visible = stripAnsi(ansiText).length();
        int pad = Math.max(0, boxWidth - visible);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(' ');
        return sb.toString();
    }

    /** Strips ANSI escape sequences to get the visible length. */
    private static String stripAnsi(String text) {
        return text.replaceAll("\\033\\[[0-9;]*m", "").replaceAll("[^\\x00-\\x7F]", "?");
    }
}
