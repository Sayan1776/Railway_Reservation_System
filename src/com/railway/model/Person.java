package com.railway.model;

import java.time.LocalDateTime;

/** Base class for everyone who can log in. User and Admin differ only in role. */
public abstract class Person {

    public enum Role { USER, ADMIN }

    private final Long id;                  // null until the database assigns one
    private String fullName;
    private final String email;             // stored lowercase
    private String phone;
    private final String passwordHash;
    private final String salt;
    private final LocalDateTime createdAt;  // null until loaded from the database

    protected Person(Long id, String fullName, String email, String phone,
                     String passwordHash, String salt, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = Require.text(fullName, "fullName");
        this.email = Require.text(email, "email").toLowerCase();
        this.phone = requirePhone(phone);
        this.passwordHash = Require.text(passwordHash, "passwordHash");
        this.salt = Require.text(salt, "salt");
        this.createdAt = createdAt;
    }

    /** Each subclass says which role it represents. */
    public abstract Role getRole();

    public boolean isAdmin() {
        return getRole() == Role.ADMIN;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setFullName(String fullName) {
        this.fullName = Require.text(fullName, "fullName");
    }

    public void setPhone(String phone) {
        this.phone = requirePhone(phone);
    }

    private static String requirePhone(String phone) {
        String p = Require.text(phone, "phone");
        if (!p.matches("[0-9]{10}")) {
            throw new IllegalArgumentException("phone must be exactly 10 digits");
        }
        return p;
    }

    /** Never prints the hash or salt. */
    @Override
    public String toString() {
        return getRole() + "{id=" + id + ", name=" + fullName + ", email=" + email + "}";
    }
}