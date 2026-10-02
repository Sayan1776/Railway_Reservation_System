package com.railway.repository.jdbc;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Simple JDBC connection pool for Supabase PostgreSQL.
 * <p>
 * Maintains a fixed-size pool of reusable connections. When a connection is
 * requested, it is taken from the pool; when it is closed (via the wrapper),
 * it is returned to the pool instead of being destroyed.
 * </p>
 * <p>
 * This avoids the overhead of creating a new TCP + TLS connection to Supabase
 * on every single database call, which was the main performance bottleneck.
 * </p>
 */
public class DBConnection {

    private static final int POOL_SIZE = 5;
    private static final Properties props = new Properties();
    private static final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(POOL_SIZE);
    private static volatile boolean initialized = false;

    static {
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                "Cannot read db.properties. Run from the project root.");
        }
    }

    private DBConnection() {}

    /**
     * Returns a pooled connection. The caller MUST close it in a try-with-resources
     * block; closing returns it to the pool rather than destroying it.
     */
    public static Connection getConnection() throws SQLException {
        initPool();
        Connection raw = pool.poll();
        if (raw == null || raw.isClosed()) {
            raw = createNewConnection();
        }
        return new PooledConnection(raw, pool);
    }

    private static synchronized void initPool() throws SQLException {
        if (initialized) return;
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.offer(createNewConnection());
        }
        initialized = true;
    }

    private static Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.user"),
            props.getProperty("db.password"));
    }
}