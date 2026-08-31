package ru.itmo.episland.zone;

import java.time.Instant;

public record ZoneAssignment(
    long id,
    String residentId,
    String fullName,
    long zoneId,
    String zoneName,
    int transformationPercent,
    Instant assignedAt,
    Instant updatedAt
) {
    public TransformationStage stage() {
        return TransformationStage.fromPercent(transformationPercent);
    }
}
