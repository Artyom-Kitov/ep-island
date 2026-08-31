package ru.itmo.episland.referral;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistryLookup(
    boolean found,
    String fullName,
    LocalDate birthDate,
    BigDecimal debtAmount,
    String reason,
    String documents
) {
    public static RegistryLookup missing(String fullName) {
        return new RegistryLookup(false, fullName, null, null, null, null);
    }
}
