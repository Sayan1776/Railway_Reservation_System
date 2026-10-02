package com.railway.repository.jdbc;

import com.railway.exception.DataAccessException;
import com.railway.model.Admin;
import com.railway.model.Person;
import com.railway.model.User;
import com.railway.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    private static final String COLUMNS =
            "user_id, full_name, email, phone, password_hash, salt, role, created_at";

    @Override
    public Optional<Person> findByEmail(String email) {
        return findOne("select " + COLUMNS + " from users where email = ?",
                ps -> ps.setString(1, email.trim().toLowerCase()), "email " + email);
    }

    @Override
    public Optional<Person> findById(long id) {
        return findOne("select " + COLUMNS + " from users where user_id = ?",
                ps -> ps.setLong(1, id), "id " + id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("select 1 from users where email = ?", ps -> ps.setString(1, email.trim().toLowerCase()));
    }

    @Override
    public boolean adminExists() {
        return exists("select 1 from users where role = 'ADMIN' limit 1", ps -> { });
    }

    @Override
    public Person save(Person p) {
        String sql = "insert into users (full_name, email, phone, password_hash, salt, role) "
                   + "values (?, ?, ?, ?, ?, ?) returning user_id, created_at";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getFullName());
            ps.setString(2, p.getEmail());
            ps.setString(3, p.getPhone());
            ps.setString(4, p.getPasswordHash());
            ps.setString(5, p.getSalt());
            ps.setString(6, p.getRole().name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Long id = rs.getLong("user_id");
                LocalDateTime created = toLocal(rs.getObject("created_at", OffsetDateTime.class));
                return p.isAdmin()
                        ? new Admin(id, p.getFullName(), p.getEmail(), p.getPhone(), p.getPasswordHash(), p.getSalt(), created)
                        : new User(id, p.getFullName(), p.getEmail(), p.getPhone(), p.getPasswordHash(), p.getSalt(), created);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save user " + p.getEmail(), e);
        }
    }

    // ----------------------------------------------------------------- helpers

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private Optional<Person> findOne(String sql, Binder binder, String what) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapPerson(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user by " + what, e);
        }
    }

    private boolean exists(String sql, Binder binder) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("User lookup failed", e);
        }
    }

    private Person mapPerson(ResultSet rs) throws SQLException {
        Long id = rs.getLong("user_id");
        String name = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String hash = rs.getString("password_hash");
        String salt = rs.getString("salt");
        LocalDateTime created = toLocal(rs.getObject("created_at", OffsetDateTime.class));
        return "ADMIN".equals(rs.getString("role"))
                ? new Admin(id, name, email, phone, hash, salt, created)
                : new User(id, name, email, phone, hash, salt, created);
    }

    private static LocalDateTime toLocal(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}