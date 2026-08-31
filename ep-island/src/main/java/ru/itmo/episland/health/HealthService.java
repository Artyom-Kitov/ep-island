package ru.itmo.episland.health;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HealthService {
    private final HealthRepository repository;

    public HealthService(HealthRepository repository) {
        this.repository = repository;
    }

    public Health health() {
        HealthStatus status = repository.databaseAvailable() ? HealthStatus.UP : HealthStatus.DOWN;
        return new Health(status, Instant.now());
    }
}
