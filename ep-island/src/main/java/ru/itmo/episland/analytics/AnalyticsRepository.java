package ru.itmo.episland.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.itmo.episland.referral.ReferralStatus;
import ru.itmo.episland.zone.TransformationStage;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AnalyticsRepository {
    private static final String REPORT_SELECT = """
        SELECT p.id AS resident_id, d.full_name, r.id AS referral_id, r.status AS referral_status,
               z.name AS zone_name, COALESCE(a.transformation_percent, 0) AS transformation_percent,
               s.wool_kg, s.predicted_energy_kwh, r.created_by AS officer, s.completed_by AS engineer,
               CAST(p.arrived_at AS DATE) AS arrived_date
        FROM ep_resident p
        JOIN ep_referral r ON r.id = p.referral_id
        JOIN ep_debtor d ON d.id = r.debtor_id
        LEFT JOIN ep_zone_assignment a ON a.resident_id = p.id AND a.left_at IS NULL
        LEFT JOIN ep_zone z ON z.id = a.zone_id
        LEFT JOIN ep_shearing s ON s.resident_id = p.id
        """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public AnalyticsRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    public Dashboard loadDashboard() {
        return new Dashboard(
            scalar("SELECT COUNT(*) FROM ep_referral WHERE status <> 'CANCELLED'"),
            scalar("SELECT COUNT(*) FROM ep_resident WHERE status = 'ARRIVED'"),
            scalar("SELECT COUNT(*) FROM ep_resident WHERE status = 'ASSIGNED'"),
            scalar("SELECT COUNT(*) FROM ep_resident WHERE status = 'TRANSFORMED'"),
            decimal("SELECT COALESCE(SUM(wool_kg), 0) FROM ep_shearing WHERE status = 'COMPLETED'"),
            decimal("SELECT COALESCE(SUM(actual_kwh), 0) FROM ep_energy_shift"),
            scalar("SELECT COUNT(*) FROM ep_energy_shift WHERE delivery_status = 'PENDING'"),
            jdbc.query("""
                SELECT z.id, z.name, z.capacity, COUNT(a.id) AS occupied,
                       ROUND(COUNT(a.id) * 100.0 / z.capacity) AS load_percent
                FROM ep_zone z
                LEFT JOIN ep_zone_assignment a ON a.zone_id = z.id AND a.left_at IS NULL
                GROUP BY z.id, z.name, z.capacity ORDER BY z.id
                """, (rs, rowNum) -> new ZoneLoad(
                    rs.getLong("id"), rs.getString("name"), rs.getInt("capacity"),
                    rs.getInt("occupied"), rs.getInt("load_percent")))
        );
    }

    public List<ReportRow> findReportRows(ReportFilter filter) {
        StringBuilder sql = new StringBuilder(REPORT_SELECT);
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("from", filter.from())
            .addValue("to", filter.to().plusDays(1));
        conditions.add("p.arrived_at >= :from");
        conditions.add("p.arrived_at < :to");

        if (filter.zoneId() != null) {
            conditions.add("a.zone_id = :zoneId");
            parameters.addValue("zoneId", filter.zoneId());
        }
        addStageCondition(conditions, filter.stage());
        addEquals(conditions, parameters, "r.status", "referralStatus",
            filter.referralStatus() == null ? null : filter.referralStatus().name());
        addEquals(conditions, parameters, "r.created_by", "officer", filter.officer());
        addEquals(conditions, parameters, "s.completed_by", "engineer", filter.engineer());

        sql.append(" WHERE ").append(String.join(" AND ", conditions));
        sql.append(" ORDER BY p.arrived_at DESC, p.id");
        return namedJdbc.query(sql.toString(), parameters, AnalyticsRepository::mapReport);
    }

    private static void addStageCondition(List<String> conditions, TransformationStage stage) {
        if (stage == null) {
            return;
        }
        conditions.add(switch (stage) {
            case INITIAL -> "COALESCE(a.transformation_percent, 0) < 34";
            case INTERMEDIATE -> "a.transformation_percent BETWEEN 34 AND 99";
            case COMPLETED -> "a.transformation_percent = 100";
        });
    }

    private static void addEquals(List<String> conditions, MapSqlParameterSource parameters,
                                  String column, String name, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(column + " = :" + name);
            parameters.addValue(name, value.trim());
        }
    }

    private long scalar(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private BigDecimal decimal(String sql) {
        BigDecimal value = jdbc.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static ReportRow mapReport(ResultSet rs, int rowNum) throws SQLException {
        return new ReportRow(
            rs.getString("resident_id"),
            rs.getString("full_name"),
            rs.getLong("referral_id"),
            ReferralStatus.valueOf(rs.getString("referral_status")),
            rs.getString("zone_name"),
            rs.getInt("transformation_percent"),
            rs.getBigDecimal("wool_kg"),
            rs.getBigDecimal("predicted_energy_kwh"),
            rs.getString("officer"),
            rs.getString("engineer"),
            rs.getDate("arrived_date").toLocalDate()
        );
    }
}
