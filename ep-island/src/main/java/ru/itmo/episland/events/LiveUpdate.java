package ru.itmo.episland.events;

import java.time.Instant;

public record LiveUpdate(LiveUpdateScope scope, String entityId, Instant occurredAt) {
    public static LiveUpdate of(LiveUpdateScope scope, Object entityId) {
        return new LiveUpdate(scope, entityId == null ? null : entityId.toString(), Instant.now());
    }
}
