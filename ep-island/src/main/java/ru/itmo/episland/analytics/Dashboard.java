package ru.itmo.episland.analytics;

import java.math.BigDecimal;
import java.util.List;

public record Dashboard(
    long referrals,
    long arrived,
    long assigned,
    long transformed,
    BigDecimal woolKg,
    BigDecimal energyKwh,
    long pendingDeliveries,
    List<ZoneLoad> zones
) {
}
