package com.decisionhub.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationHelper {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void dropNotificationConstraint() {
        try {
            log.info("Checking and dropping restrictive notification constraints if they exist...");
            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_check");
            log.info("Notification constraint check complete!");
        } catch (Exception e) {
            log.warn("Database constraint modification note: {}", e.getMessage());
        }

        try {
            log.info("Ensuring users table columns and constraints for blocked accounts and avatars...");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked_at TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked_by VARCHAR(100)");
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check");
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_check");
            log.info("Users table checks complete!");
        } catch (Exception e) {
            log.warn("Database constraint modification note: {}", e.getMessage());
        }
    }
}
