package com.railway.model;

import java.time.LocalDateTime;

public class User extends Person {

    public User(Long id, String fullName, String email, String phone,
                String passwordHash, String salt, LocalDateTime createdAt) {
        super(id, fullName, email, phone, passwordHash, salt, createdAt);
    }

    @Override
    public Role getRole() {
        return Role.USER;
    }
}