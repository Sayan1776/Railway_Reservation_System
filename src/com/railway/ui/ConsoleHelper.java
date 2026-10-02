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

public class ConsoleHelper {

    // --- ANSI color codes ---
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

    // --- Layout Constants ---
    public static final int UI_WIDTH = 110; 
    public static final String MARGIN = "                                   "; // 35 spaces for ultra-wide centering

    // --- Box-drawing characters ---
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

    public static class InputClosedException extends RuntimeException {
        public InputClosedException() { super("Input closed"); }
    }

    private final Scanner in = new Scanner(System.in);

    /** Switches to the alternate screen buffer — terminal history becomes invisible. */
    public void enableAlternateBuffer() {
        System.out.print("\033[?1049h");
        System.out.flush();
    }

    /** Switches back to the main screen buffer — terminal history is restored. */
    public void disableAlternateBuffer() {
        System.out.print("\033[?1049l");
        System.out.flush();
    }

    /** Clears the current screen. */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void showLoading(String message) {
        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        try {
            for (int i = 0; i < 10; i++) {
                String frame = CYAN + frames[i % frames.length] + RESET + " " + message;
                System.out.print("\r" + MARGIN + centerText(frame, UI_WIDTH));
                Thread.sleep(80);
            }
            String done = GREEN + CHECK + RESET + " " + message;
            System.out.print("\r" + MARGIN + centerText(done, UI_WIDTH) + "\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void showProgress(String message, int durationMs) {
        int barWidth = 30;
        int steps = 20;
        int delay = durationMs / steps;
        try {
            for (int i = 0; i <= steps; i++) {
                int filled = (i * barWidth) / steps;
                int empty = barWidth - filled;
                StringBuilder bar = new StringBuilder();
                bar.append(GREEN);
                for (int j = 0; j < filled; j++) bar.append("█");
                bar.append(DIM);
                for (int j = 0; j < empty; j++) bar.append("░");
                bar.append(RESET);
                int percent = (i * 100) / steps;
                String text = String.format("%s [%s] %3d%%", message, bar, percent);
                System.out.print("\r" + MARGIN + centerText(text, UI_WIDTH));
                Thread.sleep(delay);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void typewrite(String text, int charDelayMs) {
        try {
            System.out.print(MARGIN + padRight("", (UI_WIDTH - stripAnsi(text).length()) / 2));
            for (char c : text.toCharArray()) {
                System.out.print(c);
                Thread.sleep(charDelayMs);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(MARGIN + centerText(text, UI_WIDTH));
        }
    }

    public String readLine(String prompt) {
        String p = CYAN + "  " + ARROW + " " + RESET + prompt;
        System.out.print(MARGIN + p);
        if (!in.hasNextLine()) throw new InputClosedException();
        return in.nextLine().trim();
    }

    public <T> T read(String prompt, Parser<T> parser) {
        while (true) {
            try { return parser.parse(readLine(prompt)); } 
            catch (InvalidInputException e) { System.out.println(MARGIN + "    " + RED + CROSS + " " + e.getMessage() + RESET); }
        }
    }

    public int readInt(String prompt, int min, int max) {
        return read(prompt, text -> Validator.requireInt(text, min, max, "Value"));
    }

    public LocalDate readDate(String label) {
        return read(label + " (dd-MM-yyyy): ", DateUtil::parse);
    }

    public String readSecret(String prompt) {
        return readLine(prompt);
    }

    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (" + GREEN + "y" + RESET + "/" + RED + "n" + RESET + "): ").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("n") || answer.equals("no")) return false;
            System.out.println(MARGIN + "    " + RED + CROSS + " Please answer y or n" + RESET);
        }
    }

    public int choose(String title, List<String> options, boolean allowBack) {
        System.out.println();
        System.out.println(MARGIN + BOLD + CYAN + centerText(title, UI_WIDTH) + RESET);
        thinLine();
        for (int i = 0; i < options.size(); i++) {
            System.out.println(MARGIN + "    " + YELLOW + (i + 1) + RESET + ". " + options.get(i));
        }
        if (allowBack) {
            System.out.println(MARGIN + "    " + DIM + "0. Back" + RESET);
        }
        thinLine();
        return readInt("Choose: ", allowBack ? 0 : 1, options.size()) - 1;
    }

    public <E extends Enum<E>> E chooseEnum(String title, E[] values) {
        List<String> names = new ArrayList<>();
        for (E value : values) names.add(pretty(value));
        return values[choose(title, names, false)];
    }

    public void heading(String text) {
        String pad = repeat(H_DOUBLE, UI_WIDTH);
        System.out.println();
        System.out.println(MARGIN + BOLD + CYAN + TL + pad + TR + RESET);
        System.out.println(MARGIN + BOLD + CYAN + V_LINE + RESET + BOLD + centerText(text, UI_WIDTH) + BOLD + CYAN + V_LINE + RESET);
        System.out.println(MARGIN + BOLD + CYAN + BL + pad + BR + RESET);
    }

    public void thinLine() {
        System.out.println(MARGIN + DIM + repeat(H_LINE, UI_WIDTH + 2) + RESET);
    }

    public void success(String message) { System.out.println(MARGIN + "    " + GREEN + CHECK + " " + message + RESET); }
    public void info(String message) { System.out.println(MARGIN + "    " + CYAN + DOT + " " + message + RESET); }
    public void warn(String message) { System.out.println(MARGIN + "    " + YELLOW + "⚠ " + message + RESET); }
    public void error(Exception e) {
        if (e instanceof InputClosedException) throw (InputClosedException) e;
        String msg = e instanceof DataAccessException ? "Database problem: " + e.getMessage() : e.getMessage();
        System.out.println(MARGIN + "    " + RED + CROSS + " " + msg + RESET);
    }

    public void printTicket(Ticket t) {
        String border = repeat(H_DOUBLE, UI_WIDTH);
        String thin   = repeat(H_LINE, UI_WIDTH);
        System.out.println();
        System.out.println(MARGIN + BOLD + CYAN + TL + border + TR + RESET);
        System.out.println(MARGIN + BOLD + CYAN + V_LINE + RESET + centerText(TICKET_ICON + " RAILWAY TICKET " + TICKET_ICON, UI_WIDTH) + BOLD + CYAN + V_LINE + RESET);
        System.out.println(MARGIN + BOLD + CYAN + T_LEFT + border + T_RIGHT + RESET);
        
        printField(UI_WIDTH, "PNR",      BOLD + YELLOW + t.getPnr() + RESET);
        printField(UI_WIDTH, "Train",    String.valueOf(t.getTrainNumber()));
        printField(UI_WIDTH, "Journey",  t.getFromStation() + " → " + t.getToStation() + " on " + DateUtil.format(t.getJourneyDate()));
        printField(UI_WIDTH, "Class",    t.getSeatClass().getDisplayName() + " (" + t.getSeatClass().getCode() + ")");
        String statusColor = t.isCancelled() ? RED : GREEN;
        printField(UI_WIDTH, "Status",   statusColor + t.getStatus() + RESET);
        printField(UI_WIDTH, "Fare",     GREEN + "Rs. " + t.getTotalFare() + RESET);
        
        if (t.isCancelled()) {
            printField(UI_WIDTH, "Refund", YELLOW + "Rs. " + t.getRefundAmount() + RESET + DIM + " (" + DateUtil.formatDateTime(t.getCancelledAt()) + ")" + RESET);
        }
        
        System.out.println(MARGIN + DIM + V_LINE + thin + V_LINE + RESET);
        System.out.println(MARGIN + BOLD + V_LINE + "  Passengers:" + padRight("", UI_WIDTH - 13) + V_LINE + RESET);
        int number = 1;
        for (Passenger p : t.getPassengers()) {
            String seat = p.getSeatLabel() != null ? p.getSeatLabel() : "Pending";
            String pInfo = "  " + number++ + ". " + p + " [" + pretty(p.getBerthPreference()) + "] - Seat: " + GREEN + seat + RESET;
            System.out.println(MARGIN + V_LINE + pInfo + padForBox(pInfo, UI_WIDTH) + V_LINE);
        }
        System.out.println(MARGIN + BOLD + CYAN + BL + border + BR + RESET);
    }

    private void printField(int boxWidth, String label, String value) {
        String content = "  " + DIM + label + RESET + padRight("", 10 - label.length()) + ": " + value;
        System.out.println(MARGIN + V_LINE + content + padForBox(content, boxWidth) + V_LINE);
    }

    public void printBanner() {
        clearScreen();
        String border = repeat(H_DOUBLE, UI_WIDTH);
        System.out.println(MARGIN + BOLD + CYAN + TL + border + TR + RESET);
        
        String[] art = {
            BOLD + WHITE +  "      ____       _ _                                  ",
            BOLD + WHITE +  "     |  _ \\ __ _(_) |_      ____ _ _   _              ",
            BOLD + WHITE +  "     | |_) / _ | | \\ \\ /\\ / / _ | | | |             ",
            BOLD + WHITE +  "     |  _ < (_| | | |\\ V  V / (_| | |_| |             ",
            BOLD + WHITE +  "     |_| \\_\\__,_|_|_| \\_/\\_/ \\__,_|\\__, |             ",
            BOLD + WHITE +  "                                    |___/              ",
            BOLD + GREEN +  "       R E S E R V A T I O N   S Y S T E M             ",
            "",
            DIM +           "       " + TRAIN + "  Fast • Reliable • Convenient             "
        };
        
        for (String line : art) {
            System.out.println(MARGIN + BOLD + CYAN + V_LINE + RESET + centerText(line, UI_WIDTH) + BOLD + CYAN + V_LINE + RESET);
        }
        System.out.println(MARGIN + BOLD + CYAN + BL + border + BR + RESET);
    }

    public void printGoodbye() {
        System.out.println();
        thinLine();
        typewrite("Thank you for using Railway Reservation System!", 20);
        typewrite("Have a safe journey! " + TRAIN, 20);
        thinLine();
        System.out.println();
    }

    public int showMenu(String title, String[] options, int min, int max) {
        String border = repeat(H_DOUBLE, UI_WIDTH);
        String thin   = repeat(H_LINE, UI_WIDTH);

        System.out.println();
        System.out.println(MARGIN + BOLD + CYAN + TL + border + TR + RESET);
        System.out.println(MARGIN + BOLD + CYAN + V_LINE + RESET + centerText(title, UI_WIDTH) + BOLD + CYAN + V_LINE + RESET);
        System.out.println(MARGIN + CYAN + T_LEFT + thin + T_RIGHT + RESET);
        
        // Calculate padding to center the menu options block
        int longestOpt = 0;
        for (String opt : options) longestOpt = Math.max(longestOpt, stripAnsi(opt).length());
        int leftPad = (UI_WIDTH - longestOpt) / 2;
        
        for (String opt : options) {
            String paddedOpt = padRight("", leftPad) + opt;
            System.out.println(MARGIN + V_LINE + paddedOpt + padForBox(paddedOpt, UI_WIDTH) + V_LINE);
        }
        System.out.println(MARGIN + BOLD + CYAN + BL + border + BR + RESET);

        return readInt("Choose: ", min, max);
    }

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

    private String padForBox(String ansiText, int boxWidth) {
        int visible = stripAnsi(ansiText).length();
        int pad = Math.max(0, boxWidth - visible);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(' ');
        return sb.toString();
    }

    public static String stripAnsi(String text) {
        return text.replaceAll("\\033\\[[0-9;]*m", "").replaceAll("[^\\x00-\\x7F]", "?");
    }
}






