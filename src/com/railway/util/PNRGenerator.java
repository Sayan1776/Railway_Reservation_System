package com.railway.util;

import java.security.SecureRandom;

public final class PNRGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PNRGenerator() {}

    /** A random 10-digit PNR that never starts with 0. Uniqueness is checked by the caller. */
    public static String generate() {
        long n = 1_000_000_000L + Math.abs(RANDOM.nextLong() % 9_000_000_000L);
        return Long.toString(n);
    }
}