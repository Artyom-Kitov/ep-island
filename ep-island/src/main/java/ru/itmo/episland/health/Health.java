package ru.itmo.episland.health;

import java.time.Instant;

public record Health(HealthStatus status, Instant time) {
}
