package ru.itmo.episland.referral;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ReferralRepository {
    private static final String BASE_SELECT = """
        SELECT r.id, r.debtor_id, d.full_name, d.birth_date, d.debt_amount, d.reason, d.documents,
               r.status, r.created_by, r.created_at
        FROM ep_referral r
        JOIN ep_debtor d ON d.id = r.debtor_id
        """;

    private final JdbcTemplate jdbc;

    public ReferralRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Referral> findAll() {
        return jdbc.query(BASE_SELECT + " ORDER BY r.created_at DESC", ReferralRepository::map);
    }

    public Optional<Referral> findById(long id) {
        return first(jdbc.query(BASE_SELECT + " WHERE r.id = ?", ReferralRepository::map, id));
    }

    public Optional<Referral> findByIdempotencyKey(String key) {
        return first(jdbc.query(BASE_SELECT + " WHERE r.idempotency_key = ?", ReferralRepository::map, key));
    }

    public long insertDebtor(CreateReferralData data, String actor) {
        Long id = jdbc.queryForObject("""
            INSERT INTO ep_debtor(full_name, birth_date, debt_amount, reason, documents, created_by)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """, Long.class,
            data.fullName().trim(),
            data.birthDate() == null ? null : Date.valueOf(data.birthDate()),
            data.debtAmount(),
            data.reason().trim(),
            blankToNull(data.documents()),
            actor);
        return requireId(id, "должника");
    }

    public long insertReferral(long debtorId, String idempotencyKey, String actor) {
        Long id = jdbc.queryForObject("""
            INSERT INTO ep_referral(debtor_id, idempotency_key, created_by)
            VALUES (?, ?, ?) RETURNING id
            """, Long.class, debtorId, idempotencyKey, actor);
        return requireId(id, "направления");
    }

    public void updateStatus(long id, ReferralStatus status) {
        jdbc.update("UPDATE ep_referral SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            status.name(), id);
    }

    public void delete(long referralId, long debtorId) {
        jdbc.update("DELETE FROM ep_referral WHERE id = ?", referralId);
        jdbc.update("DELETE FROM ep_debtor WHERE id = ?", debtorId);
    }

    private static Optional<Referral> first(List<Referral> referrals) {
        return referrals.stream().findFirst();
    }

    private static long requireId(Long id, String entity) {
        if (id == null) {
            throw new IllegalStateException("База данных не вернула ID " + entity);
        }
        return id;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Referral map(ResultSet rs, int rowNum) throws SQLException {
        Date birthDate = rs.getDate("birth_date");
        return new Referral(
            rs.getLong("id"),
            rs.getLong("debtor_id"),
            rs.getString("full_name"),
            birthDate == null ? null : birthDate.toLocalDate(),
            rs.getBigDecimal("debt_amount"),
            rs.getString("reason"),
            rs.getString("documents"),
            ReferralStatus.valueOf(rs.getString("status")),
            rs.getString("created_by"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
