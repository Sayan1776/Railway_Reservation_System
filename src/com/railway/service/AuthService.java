package com.railway.service;

import com.railway.exception.AuthenticationException;
import com.railway.exception.DataAccessException;
import com.railway.exception.InvalidInputException;
import com.railway.model.Admin;
import com.railway.model.Person;
import com.railway.model.User;
import com.railway.repository.UserRepository;
import com.railway.util.PasswordHasher;
import com.railway.util.Validator;

import java.util.Optional;

public class AuthService {

    private static final String BAD_LOGIN = "Invalid email or password";
    /** Hashed when the email is unknown, so "no such user" takes as long as "wrong password". */
    private static final String DUMMY_SALT = PasswordHasher.newSalt();

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public User register(String fullName, String email, String phone, String password)
            throws InvalidInputException {
        String n = Validator.requireName(fullName);
        String e = Validator.requireEmail(email);
        String ph = Validator.requirePhone(phone);
        Validator.requirePassword(password);
        if (users.existsByEmail(e)) {
            throw new InvalidInputException("An account with this email already exists");
        }
        String salt = PasswordHasher.newSalt();
        return (User) saveOrExplain(new User(null, n, e, ph, PasswordHasher.hash(password, salt), salt, null));
    }

    /** True on a fresh database: the first run should ask for an admin password. */
    public boolean needsAdminSetup() {
        return !users.adminExists();
    }

    /** Only works while no admin exists. */
    public Admin createFirstAdmin(String fullName, String email, String phone, String password)
            throws InvalidInputException {
        if (users.adminExists()) {
            throw new InvalidInputException("An admin already exists");
        }
        String n = Validator.requireName(fullName);
        String e = Validator.requireEmail(email);
        String ph = Validator.requirePhone(phone);
        Validator.requirePassword(password);
        if (users.existsByEmail(e)) {
            throw new InvalidInputException("An account with this email already exists");
        }
        String salt = PasswordHasher.newSalt();
        return (Admin) saveOrExplain(new Admin(null, n, e, ph, PasswordHasher.hash(password, salt), salt, null));
    }

    /** Returns a User or an Admin. Same message for an unknown email and a wrong password. */
    public Person login(String email, String password) throws AuthenticationException {
        if (email == null || password == null) {
            throw new AuthenticationException(BAD_LOGIN);
        }
        Optional<Person> found = users.findByEmail(email);
        if (!found.isPresent()) {
            PasswordHasher.hash(password, DUMMY_SALT);
            throw new AuthenticationException(BAD_LOGIN);
        }
        Person person = found.get();
        if (!PasswordHasher.matches(password, person.getSalt(), person.getPasswordHash())) {
            throw new AuthenticationException(BAD_LOGIN);
        }
        return person;
    }

    /** Two people registering the same email at once: the database rejects the second one. */
    private Person saveOrExplain(Person person) throws InvalidInputException {
        try {
            return users.save(person);
        } catch (DataAccessException ex) {
            if (users.existsByEmail(person.getEmail())) {
                throw new InvalidInputException("An account with this email already exists");
            }
            throw ex;
        }
    }
}