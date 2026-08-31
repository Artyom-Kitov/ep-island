package ru.itmo.episland.analytics;

import ru.itmo.episland.referral.ReferralStatus;
import ru.itmo.episland.zone.TransformationStage;

import java.time.LocalDate;

public record ReportFilter(
    LocalDate from,
    LocalDate to,
    Long zoneId,
    TransformationStage stage,
    ReferralStatus referralStatus,
    String officer,
    String engineer
) {
}
