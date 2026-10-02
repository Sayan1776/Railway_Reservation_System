package com.railway.model;

/** Package-private validation helpers shared by the model classes. */
final class Require {

    private Require() {}

    static String text(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    static int atLeast(int value, int min, String field) {
        if (value < min) {
            throw new IllegalArgumentException(field + " must be at least " + min);
        }
        return value;
    }
}