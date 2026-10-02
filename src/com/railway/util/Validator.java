package com.railway.util;

import com.railway.exception.InvalidInputException;

import java.util.regex.Pattern;

/** Checks text typed by a person. Each method returns the cleaned value or throws InvalidInputException. */
public final class Validator {

    private static final Pattern NAME = Pattern.compile("^[\\p{L} .'-]{2,60}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[6-9][0-9]{9}$");

    private Validator() {}

    public static String requireName(String value) throws InvalidInputException {
        String v = value == null ? "" : value.trim();
        if (!NAME.matcher(v).matches()) {
            throw new InvalidInputException("Name must be 2 to 60 characters: letters, spaces, . ' -");
        }
        return v;
    }

    /** Returns the email trimmed and lowercased. */
    public static String requireEmail(String value) throws InvalidInputException {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (!EMAIL.matcher(v).matches()) {
            throw new InvalidInputException("Enter a valid email address");
        }
        return v;
    }

    public static String requirePhone(String value) throws InvalidInputException {
        String v = value == null ? "" : value.trim();
        if (!PHONE.matcher(v).matches()) {
            throw new InvalidInputException("Phone must be a 10-digit mobile number starting with 6 to 9");
        }
        return v;
    }

    public static void requirePassword(String value) throws InvalidInputException {
        if (value == null || value.length() < 8
                || value.chars().noneMatch(Character::isLetter)
                || value.chars().noneMatch(Character::isDigit)) {
            throw new InvalidInputException("Password needs at least 8 characters with a letter and a digit");
        }
    }

    public static int requireInt(String text, int min, int max, String label) throws InvalidInputException {
        try {
            int n = Integer.parseInt(text == null ? "" : text.trim());
            if (n < min || n > max) {
                throw new InvalidInputException(label + " must be between " + min + " and " + max);
            }
            return n;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(label + " must be a whole number");
        }
    }
}