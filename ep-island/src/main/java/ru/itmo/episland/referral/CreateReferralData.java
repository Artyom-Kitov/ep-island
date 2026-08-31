package ru.itmo.episland.referral;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateReferralData(
    String fullName,
    LocalDate birthDate,
    BigDecimal debtAmount,
    String reason,
    String documents
) {
}
