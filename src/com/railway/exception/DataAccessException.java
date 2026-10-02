package com.railway.exception;

/**
 * Thrown by repositories when the database fails (connection lost, bad SQL, ...).
 * Unchecked on purpose: services and menus don't need a try/catch around every call,
 * and no java.sql type leaks above the repository layer.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}