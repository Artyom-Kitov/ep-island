package ru.itmo.episland.energy;

import java.math.BigDecimal;
import java.time.Instant;

public record Shearing(
    long id,
    String residentId,
    String fullName,
    BigDecimal woolKg,
    BigDecimal predictedEnergyKwh,
    ShearingStatus status,
    Instant updatedAt
) {
}
