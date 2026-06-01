package com.cmulagent.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @PostConstruct
    public void enableWALMode() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data/cmulagent.db");
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA busy_timeout=5000");
            log.info("SQLite pragma configured: WAL mode, foreign keys ON, busy timeout 5000ms");

            ClassPathResource resource = new ClassPathResource("schema.sql");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            String[] statements = sb.toString().split(";");
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            log.info("schema.sql executed successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SQLite", e);
        }
    }
}