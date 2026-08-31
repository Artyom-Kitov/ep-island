package ru.itmo.episland.energy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class EnergyRepository {
    private static final String SHEARING_SELECT = """
        SELECT s.id, s.resident_id, d.full_name, s.wool_kg, s.predicted_energy_kwh,
               s.status, s.updated_at
        FROM ep_shearing s
        JOIN ep_resident p ON p.id = s.resident_id
        JOIN ep_referral r ON r.id = p.referral_id
        JOIN ep_debtor d ON d.id = r.debtor_id
        """;

    private final JdbcTemplate jdbc;

    public EnergyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Shearing> findAllShearings() {
        return jdbc.query(SHEARING_SELECT + " ORDER BY s.updated_at DESC", EnergyRepository::mapShearing);
    }

    public Optional<Shearing> findShearing(String residentId) {
        return first(jdbc.query(SHEARING_SELECT + " WHERE s.resident_id = ?",
            EnergyRepository::mapShearing, residentId));
    }

    public void completeShearing(String residentId, BigDecimal woolKg,
                                 BigDecimal predictedEnergyKwh, String actor) {
        jdbc.update("""
            UPDATE ep_shearing
            SET wool_kg = ?, predicted_energy_kwh = ?, status = ?, completed_by = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE resident_id = ?
            """, woolKg, predictedEnergyKwh, ShearingStatus.COMPLETED.name(), actor, residentId);
    }

    public List<EnergyShift> findAllShifts() {
        return jdbc.query("SELECT * FROM ep_energy_shift ORDER BY created_at DESC",
            EnergyRepository::mapShift);
    }

    public Optional<EnergyShift> findShift(long id) {
        return first(jdbc.query("SELECT * FROM ep_energy_shift WHERE id = ?",
            EnergyRepository::mapShift, id));
    }

    public long insertShift(String shiftCode, BigDecimal actualKwh, String actor) {
        Long id = jdbc.queryForObject("""
            INSERT INTO ep_energy_shift(
                shift_code, actual_kwh, delivery_status, delivery_attempts, created_by
            ) VALUES (?, ?, ?, 0, ?) RETURNING id
            """, Long.class, shiftCode, actualKwh, AccountingDeliveryStatus.PENDING.name(), actor);
        return requireId(id, "смены");
    }

    public void updateShiftForCorrection(long id, BigDecimal actualKwh) {
        jdbc.update("""
            UPDATE ep_energy_shift
            SET actual_kwh = ?, delivery_status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, actualKwh, AccountingDeliveryStatus.PENDING.name(), id);
    }

    public void recordDeliveryAttempt(long id, AccountingDeliveryStatus status) {
        jdbc.update("""
            UPDATE ep_energy_shift
            SET delivery_status = ?, delivery_attempts = delivery_attempts + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, status.name(), id);
    }

    public List<Long> findPendingShiftIds(int limit) {
        return jdbc.query("""
            SELECT id FROM ep_energy_shift
            WHERE delivery_status = ?
            ORDER BY updated_at, id
            LIMIT ?
            """, (rs, rowNum) -> rs.getLong("id"), AccountingDeliveryStatus.PENDING.name(), limit);
    }

    private static <T> Optional<T> first(List<T> rows) {
        return rows.stream().findFirst();
    }

    private static long requireId(Long id, String entity) {
        if (id == null) {
            throw new IllegalStateException("База данных не вернула ID " + entity);
        }
        return id;
    }

    private static Shearing mapShearing(ResultSet rs, int rowNum) throws SQLException {
        return new Shearing(
            rs.getLong("id"),
            rs.getString("resident_id"),
            rs.getString("full_name"),
            rs.getBigDecimal("wool_kg"),
            rs.getBigDecimal("predicted_energy_kwh"),
            ShearingStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private static EnergyShift mapShift(ResultSet rs, int rowNum) throws SQLException {
        return new EnergyShift(
            rs.getLong("id"),
            rs.getString("shift_code"),
            rs.getBigDecimal("actual_kwh"),
            AccountingDeliveryStatus.valueOf(rs.getString("delivery_status")),
            rs.getInt("delivery_attempts"),
            rs.getString("created_by"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
