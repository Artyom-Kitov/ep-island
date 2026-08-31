package ru.itmo.episland.resident;

import java.time.Instant;

public record Resident(
    String id,
    long referralId,
    String fullName,
    ResidentStatus status,
    Instant arrivedAt,
    String updatedBy
) {
}
