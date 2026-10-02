package com.railway.repository;

import com.railway.model.Person;

import java.util.Optional;

/** Persistence for users and admins. Throws DataAccessException on DB failure. */
public interface UserRepository {

    /** Returns a User or an Admin depending on the stored role. Email match ignores case. */
    Optional<Person> findByEmail(String email);

    Optional<Person> findById(long id);

    boolean existsByEmail(String email);

    boolean adminExists();

    /** Inserts the person and returns a copy that has the generated id and creation time. */
    Person save(Person person);
}