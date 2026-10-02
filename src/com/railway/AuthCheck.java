package com.railway;

import com.railway.model.Admin;
import com.railway.model.Person;
import com.railway.model.User;
import com.railway.repository.UserRepository;
import com.railway.repository.jdbc.JdbcUserRepository;
import com.railway.service.AuthService;

/** Throwaway check of registration and login. Do not commit. */
public class AuthCheck {

    interface Action { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        UserRepository repo = new JdbcUserRepository();
        AuthService auth = new AuthService(repo);

        User u = auth.register("Auth Check", "Auth.Check@Example.com", "9123456780", "Secret123");
        System.out.println("1. registered: " + u + " (expect role USER, id set)");
        System.out.println("   stored hash differs from password: "
                + !u.getPasswordHash().equals("Secret123") + " (expect true)");

        blocked("2. duplicate email", () -> auth.register("Auth Check", "auth.check@example.com", "9123456780", "Secret123"));
        blocked("3a. bad email", () -> auth.register("Bad Email", "not-an-email", "9123456781", "Secret123"));
        blocked("3b. weak password", () -> auth.register("Weak Pass", "weak@example.com", "9123456782", "abc"));
        blocked("3c. bad phone", () -> auth.register("Bad Phone", "phone@example.com", "12345", "Secret123"));

        Person p = auth.login("AUTH.check@example.com", "Secret123");
        System.out.println("4. login ok, email typed in different case: " + p + " (expect same id as 1)");

        blocked("5a. wrong password", () -> auth.login("auth.check@example.com", "Wrong1234"));
        blocked("5b. unknown email", () -> auth.login("nobody@example.com", "Secret123"));

        if (auth.needsAdminSetup()) {
            Admin a = auth.createFirstAdmin("Check Admin", "check.admin@example.com", "9000000001", "Admin12345");
            Person ap = auth.login("check.admin@example.com", "Admin12345");
            System.out.println("6a. first admin created and logged in: " + ap + " | isAdmin " + ap.isAdmin()
                    + " (expect true) | same id: " + a.getId().equals(ap.getId()));
            blocked("6b. second first-admin", () ->
                    auth.createFirstAdmin("Another Admin", "another@example.com", "9000000002", "Admin12345"));
        } else {
            System.out.println("6. an admin already exists, skipped admin steps");
        }
    }

    static void blocked(String label, Action action) {
        try {
            action.run();
            System.out.println(label + ": FAIL, no exception thrown");
        } catch (Exception e) {
            System.out.println(label + ": blocked (" + e.getClass().getSimpleName() + ") " + e.getMessage());
        }
    }
}