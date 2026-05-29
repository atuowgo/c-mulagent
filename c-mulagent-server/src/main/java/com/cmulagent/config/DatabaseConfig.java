package com.cmulagent.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {

    @PostConstruct
    public void enableWALMode() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data/cmulagent.db");
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA busy_timeout=5000");
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SQLite WAL mode", e);
        }
    }
}