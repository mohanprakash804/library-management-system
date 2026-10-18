package com.mohan.library.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates JDBC connections.
 * Settings are read from environment variables (DB_URL, DB_USER, DB_PASSWORD)
 * or, if those are not set, from src/main/resources/db.properties.
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() { }

    private static String setting(String envName, String propName) {
        String value = System.getenv(envName);
        return (value != null && !value.isBlank()) ? value : PROPS.getProperty(propName);
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("DB_URL", "db.url");
        String user = setting("DB_USER", "db.user");
        String password = setting("DB_PASSWORD", "db.password");
        if (url == null) {
            throw new SQLException("Database settings not found. Copy db.properties.example to db.properties and fill it in.");
        }
        return DriverManager.getConnection(url, user, password);
    }
}
