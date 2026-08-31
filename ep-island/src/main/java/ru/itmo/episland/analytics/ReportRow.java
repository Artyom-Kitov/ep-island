package ru.itmo.episland.analytics;

import ru.itmo.episland.referral.ReferralStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportRow(
    String residentId,
    String fullName,
    long referralId,
    ReferralStatus referralStatus,
    String zoneName,
    int transformationPercent,
    BigDecimal woolKg,
    BigDecimal predictedEnergyKwh,
    String officer,
    String engineer,
    LocalDate arrivedDate
) {
}
