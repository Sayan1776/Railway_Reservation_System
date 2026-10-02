package com.railway.util;

import com.railway.exception.InvalidInputException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class DateUtil {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm");

    private DateUtil() {}

    /** Parses dd-MM-yyyy, e.g. 12-10-2026. A date like 31-02-2026 is rejected. */
    public static LocalDate parse(String text) throws InvalidInputException {
        try {
            return LocalDate.parse(text == null ? "" : text.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Enter the date as dd-MM-yyyy, for example 12-10-2026");
        }
    }

    public static String format(LocalDate date) {
        return DATE.format(date);
    }

    public static String format(LocalTime time) {
        return time == null ? "--:--" : TIME.format(time);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME.format(dateTime);
    }
}