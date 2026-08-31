package ru.itmo.episland.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HealthRepository {
    private final JdbcTemplate jdbc;

    public HealthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean databaseAvailable() {
        Integer value = jdbc.queryForObject("SELECT 1", Integer.class);
        return value != null && value == 1;
    }
}
