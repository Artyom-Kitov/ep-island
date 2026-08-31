package ru.itmo.episland.analytics;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnalyticsService {
    private static final int MAX_REPORT_PERIOD_DAYS = 31;

    private final AnalyticsRepository repository;

    public AnalyticsService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    public Dashboard dashboard() {
        return repository.loadDashboard();
    }

    public List<ReportRow> report(ReportFilter requestedFilter) {
        return repository.findReportRows(normalize(requestedFilter));
    }

    public byte[] csv(ReportFilter requestedFilter) {
        List<ReportRow> rows = report(requestedFilter);
        StringBuilder csv = new StringBuilder(
            "\uFEFFID;ФИО;Направление;Статус направления;Зона;Трансформация,%;Шерсть,кг;Прогноз энергии,кВт·ч;Офицер;Инженер;Дата\r\n");
        for (ReportRow row : rows) {
            csv.append(cell(row.residentId())).append(';')
                .append(cell(row.fullName())).append(';')
                .append(row.referralId()).append(';')
                .append(row.referralStatus()).append(';')
                .append(cell(row.zoneName())).append(';')
                .append(row.transformationPercent()).append(';')
                .append(row.woolKg() == null ? "" : row.woolKg()).append(';')
                .append(row.predictedEnergyKwh() == null ? "" : row.predictedEnergyKwh()).append(';')
                .append(cell(row.officer())).append(';')
                .append(cell(row.engineer())).append(';')
                .append(row.arrivedDate()).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static ReportFilter normalize(ReportFilter requested) {
        LocalDate actualTo = requested.to() == null ? LocalDate.now() : requested.to();
        LocalDate actualFrom = requested.from() == null ? actualTo.minusDays(30) : requested.from();
        if (actualFrom.isAfter(actualTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Начало периода не может быть позже окончания");
        }
        if (ChronoUnit.DAYS.between(actualFrom, actualTo) > MAX_REPORT_PERIOD_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Период отчёта ограничен 31 днём");
        }
        return new ReportFilter(actualFrom, actualTo, requested.zoneId(), requested.stage(),
            requested.referralStatus(), requested.officer(), requested.engineer());
    }

    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        return text.contains(";") || text.contains("\n") || text.contains("\"")
            ? "\"" + text + "\""
            : text;
    }
}
