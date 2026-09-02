package ru.itmo.episland.resident;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ResidentRepository {
    private static final String BASE_SELECT = """
        SELECT p.id, p.referral_id, d.full_name, p.status, p.arrived_at, p.updated_by
        FROM ep_resident p
        JOIN ep_referral r ON r.id = p.referral_id
        JOIN ep_debtor d ON d.id = r.debtor_id
        """;

    private final JdbcTemplate jdbc;

    public ResidentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Resident> findAll() {
        return jdbc.query(BASE_SELECT + " ORDER BY p.arrived_at DESC", ResidentRepository::map);
    }

    public List<Resident> findAllByStatus(ResidentStatus status) {
        return jdbc.query(BASE_SELECT + " WHERE p.status = ? ORDER BY p.arrived_at DESC",
            ResidentRepository::map, status.name());
    }

    public Optional<Resident> findById(String id) {
        return first(jdbc.query(BASE_SELECT + " WHERE p.id = ?", ResidentRepository::map, id));
    }

    public Optional<Resident> findByReferralId(long referralId) {
        return first(jdbc.query(BASE_SELECT + " WHERE p.referral_id = ?", ResidentRepository::map, referralId));
    }

    public boolean existsReferralReadyForArrival(long referralId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM ep_referral WHERE id = ? AND status = 'HANDED_TO_CONVOY'",
            Integer.class, referralId);
        return count != null && count > 0;
    }

    public boolean existsById(String id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM ep_resident WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public void insert(String id, long referralId, String actor) {
        jdbc.update("INSERT INTO ep_resident(id, referral_id, updated_by) VALUES (?, ?, ?)",
            id, referralId, actor);
    }

    public void updateProfile(Resident resident, UpdateResidentData data, String actor) {
        jdbc.update("""
            UPDATE ep_debtor
            SET full_name = COALESCE(?, full_name), birth_date = COALESCE(?, birth_date)
            WHERE id = (SELECT r.debtor_id FROM ep_referral r WHERE r.id = ?)
            """, blankToNull(data.fullName()), data.birthDate(), resident.referralId());
        jdbc.update("""
            UPDATE ep_resident SET updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
            """, actor, resident.id());
    }

    public void delete(String id) {
        jdbc.update("DELETE FROM ep_resident WHERE id = ?", id);
    }

    private static Optional<Resident> first(List<Resident> rows) {
        return rows.stream().findFirst();
    }

    private static Resident map(ResultSet rs, int rowNum) throws SQLException {
        return new Resident(
            rs.getString("id"),
            rs.getLong("referral_id"),
            rs.getString("full_name"),
            ResidentStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("arrived_at").toInstant(),
            rs.getString("updated_by")
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
