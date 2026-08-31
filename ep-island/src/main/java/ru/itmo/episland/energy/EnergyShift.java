package ru.itmo.episland.energy;

import java.math.BigDecimal;
import java.time.Instant;

public record EnergyShift(
    long id,
    String shiftCode,
    BigDecimal actualKwh,
    AccountingDeliveryStatus deliveryStatus,
    int deliveryAttempts,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {
}
