package ru.itmo.episland.zone;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.itmo.episland.resident.ResidentStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ZoneRepository {
    private static final String ASSIGNMENT_SELECT = """
        SELECT a.id, a.resident_id, d.full_name, a.zone_id, z.name AS zone_name,
               a.transformation_percent, a.assigned_at, a.updated_at
        FROM ep_zone_assignment a
        JOIN ep_zone z ON z.id = a.zone_id
        JOIN ep_resident p ON p.id = a.resident_id
        JOIN ep_referral r ON r.id = p.referral_id
        JOIN ep_debtor d ON d.id = r.debtor_id
        """;

    private final JdbcTemplate jdbc;

    public ZoneRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Zone> findAllWithOccupancy() {
        return jdbc.query("""
            SELECT z.id, z.name, z.capacity, z.transformation_coefficient, z.predicted_hours, z.active,
                   COUNT(a.id) AS occupied
            FROM ep_zone z
            LEFT JOIN ep_zone_assignment a ON a.zone_id = z.id AND a.left_at IS NULL
            GROUP BY z.id, z.name, z.capacity, z.transformation_coefficient, z.predicted_hours, z.active
            ORDER BY z.id
            """, ZoneRepository::mapZone);
    }

    public List<ZoneAssignment> findActiveAssignments() {
        return jdbc.query(ASSIGNMENT_SELECT + " WHERE a.left_at IS NULL ORDER BY a.updated_at DESC",
            ZoneRepository::mapAssignment);
    }

    public Optional<ZoneAssignment> findAssignment(long id) {
        return first(jdbc.query(ASSIGNMENT_SELECT + " WHERE a.id = ?",
            ZoneRepository::mapAssignment, id));
    }

    public Optional<ZoneAssignment> findActiveAssignment(long id) {
        return first(jdbc.query(ASSIGNMENT_SELECT + " WHERE a.id = ? AND a.left_at IS NULL",
            ZoneRepository::mapAssignment, id));
    }

    public boolean isResidentEligible(String residentId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM ep_resident WHERE id = ? AND status IN ('ARRIVED', 'ASSIGNED')
            """, Integer.class, residentId);
        return count != null && count > 0;
    }

    public void closeActiveAssignment(String residentId) {
        jdbc.update("""
            UPDATE ep_zone_assignment
            SET left_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE resident_id = ? AND left_at IS NULL
            """, residentId);
    }

    public long insertAssignment(String residentId, long zoneId, String actor) {
        Long id = jdbc.queryForObject("""
            INSERT INTO ep_zone_assignment(resident_id, zone_id, updated_by)
            VALUES (?, ?, ?) RETURNING id
            """, Long.class, residentId, zoneId, actor);
        return requireId(id, "назначения");
    }

    public void updateResidentStatus(String residentId, ResidentStatus status, String actor) {
        jdbc.update("""
            UPDATE ep_resident SET status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
            """, status.name(), actor, residentId);
    }

    public void updateTransformation(long assignmentId, int percent, String actor) {
        jdbc.update("""
            UPDATE ep_zone_assignment
            SET transformation_percent = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND left_at IS NULL
            """, percent, actor, assignmentId);
    }

    public void recordTransformationHistory(long assignmentId, int previousPercent,
                                            int newPercent, String actor) {
        jdbc.update("""
            INSERT INTO ep_transformation_history(
                assignment_id, previous_percent, new_percent, changed_by
            ) VALUES (?, ?, ?, ?)
            """, assignmentId, previousPercent, newPercent, actor);
    }

    public void createShearingTask(String residentId, String actor) {
        jdbc.update("""
            INSERT INTO ep_shearing(resident_id, created_by)
            VALUES (?, ?)
            ON CONFLICT (resident_id) DO NOTHING
            """, residentId, actor);
    }

    private static Optional<ZoneAssignment> first(List<ZoneAssignment> rows) {
        return rows.stream().findFirst();
    }

    private static long requireId(Long id, String entity) {
        if (id == null) {
            throw new IllegalStateException("База данных не вернула ID " + entity);
        }
        return id;
    }

    private static Zone mapZone(ResultSet rs, int rowNum) throws SQLException {
        return new Zone(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getInt("capacity"),
            rs.getInt("occupied"),
            rs.getBigDecimal("transformation_coefficient"),
            rs.getBigDecimal("predicted_hours"),
            rs.getBoolean("active"),
            java.math.BigDecimal.ZERO
        );
    }

    private static ZoneAssignment mapAssignment(ResultSet rs, int rowNum) throws SQLException {
        return new ZoneAssignment(
            rs.getLong("id"),
            rs.getString("resident_id"),
            rs.getString("full_name"),
            rs.getLong("zone_id"),
            rs.getString("zone_name"),
            rs.getInt("transformation_percent"),
            rs.getTimestamp("assigned_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
