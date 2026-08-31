package ru.itmo.episland.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String actor, AuditAction action, AuditedEntityType entityType,
                    Object entityId, String details) {
        jdbc.update("""
            INSERT INTO ep_audit_log(actor, action, entity_type, entity_id, details)
            VALUES (?, ?, ?, ?, ?)
            """, actor, action.name(), entityType.name(),
            entityId == null ? null : entityId.toString(), details);
    }
}
