package ru.itmo.episland.referral;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record Referral(
    long id,
    long debtorId,
    String fullName,
    LocalDate birthDate,
    BigDecimal debtAmount,
    String reason,
    String documents,
    ReferralStatus status,
    String createdBy,
    Instant createdAt
) {
}
