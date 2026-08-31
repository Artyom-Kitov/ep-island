package ru.itmo.episland.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.episland.analytics.AnalyticsService;
import ru.itmo.episland.analytics.Dashboard;
import ru.itmo.episland.analytics.ReportFilter;
import ru.itmo.episland.analytics.ReportRow;
import ru.itmo.episland.auth.Access;
import ru.itmo.episland.auth.Role;
import ru.itmo.episland.referral.ReferralStatus;
import ru.itmo.episland.zone.TransformationStage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService service;
    private final Access access;

    public AnalyticsController(AnalyticsService service, Access access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(HttpServletRequest request) {
        access.user(request);
        return service.dashboard();
    }

    @GetMapping("/report")
    public List<ReportRow> report(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long zoneId,
        @RequestParam(required = false) TransformationStage stage,
        @RequestParam(required = false) ReferralStatus referralStatus,
        @RequestParam(required = false) String officer,
        @RequestParam(required = false) String engineer,
        HttpServletRequest request
    ) {
        access.require(request, Role.ANALYST);
        return service.report(filter(from, to, zoneId, stage, referralStatus, officer, engineer));
    }

    @GetMapping(value = "/report.csv", produces = "text/csv")
    public ResponseEntity<byte[]> csv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long zoneId,
        @RequestParam(required = false) TransformationStage stage,
        @RequestParam(required = false) ReferralStatus referralStatus,
        @RequestParam(required = false) String officer,
        @RequestParam(required = false) String engineer,
        HttpServletRequest request
    ) {
        access.require(request, Role.ANALYST);
        byte[] body = service.csv(filter(from, to, zoneId, stage, referralStatus, officer, engineer));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ep-island-report.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(body);
    }

    private static ReportFilter filter(LocalDate from, LocalDate to, Long zoneId,
                                       TransformationStage stage, ReferralStatus referralStatus,
                                       String officer, String engineer) {
        return new ReportFilter(from, to, zoneId, stage, referralStatus, officer, engineer);
    }
}
