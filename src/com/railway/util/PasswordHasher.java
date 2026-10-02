package com.railway.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/** Salted PBKDF2 password hashing from the JDK. Never store or log the plain password. */
public final class PasswordHasher {

    private static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt),
                ITERATIONS, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Constant-time comparison. A malformed stored salt or hash simply does not match. */
    public static boolean matches(String password, String salt, String expectedHash) {
        try {
            byte[] actual = Base64.getDecoder().decode(hash(password, salt));
            byte[] expected = Base64.getDecoder().decode(expectedHash);
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}