package ru.itmo.episland.referral;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class RegistryGateway {
    public RegistryLookup findDebtor(String fullName) {
        String normalized = fullName.trim();
        if (normalized.toLowerCase().contains("не найден")) {
            return RegistryLookup.missing(normalized);
        }

        int seed = Math.floorMod(normalized.toLowerCase().hashCode(), Integer.MAX_VALUE);
        return new RegistryLookup(
            true,
            normalized,
            LocalDate.of(1980 + seed % 25, 1 + seed % 12, 1 + seed % 27),
            BigDecimal.valueOf(5_000L + seed % 95_000L),
            "Просроченная задолженность по лунному кредиту",
            "Выписка из реестра № " + (10_000 + seed % 90_000)
        );
    }
}
