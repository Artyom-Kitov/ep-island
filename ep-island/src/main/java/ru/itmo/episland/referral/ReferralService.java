package ru.itmo.episland.referral;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.episland.service.AuditService;

import java.util.List;
import java.util.UUID;

import static ru.itmo.episland.service.AuditAction.CREATE;
import static ru.itmo.episland.service.AuditAction.DELETE;
import static ru.itmo.episland.service.AuditAction.STATUS_CHANGE;
import static ru.itmo.episland.service.AuditedEntityType.REFERRAL;

@Service
public class ReferralService {
    private final ReferralRepository repository;
    private final RegistryGateway registryGateway;
    private final AuditService audit;

    public ReferralService(ReferralRepository repository, RegistryGateway registryGateway, AuditService audit) {
        this.repository = repository;
        this.registryGateway = registryGateway;
        this.audit = audit;
    }

    public List<Referral> list() {
        return repository.findAll();
    }

    public List<Referral> searchPendingArrival(String fullName, int limit) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (!normalized.isEmpty() && normalized.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Для поиска по ФИО введите не менее двух символов");
        }
        return repository.searchPendingArrival(normalized, Math.max(1, Math.min(limit, 50)));
    }

    public Referral get(long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Направление не найдено"));
    }

    public RegistryLookup searchRegistry(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введите не менее трёх символов ФИО");
        }
        return registryGateway.findDebtor(normalized);
    }

    @Transactional
    public Referral create(CreateReferralData data, String idempotencyKey, String actor) {
        String key = normalizeKey(idempotencyKey);
        return repository.findByIdempotencyKey(key).orElseGet(() -> {
            long debtorId = repository.insertDebtor(data, actor);
            long referralId = repository.insertReferral(debtorId, key, actor);
            audit.log(actor, CREATE, REFERRAL, referralId, "Создано электронное направление");
            return get(referralId);
        });
    }

    @Transactional
    public Referral updateStatus(long id, ReferralStatus target, String actor) {
        Referral current = get(id);
        if (current.status() == ReferralStatus.HANDED_TO_CONVOY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Передача конвою необратима");
        }
        if (!current.status().isEditable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Изменять можно только созданное направление");
        }

        repository.updateStatus(id, target);
        audit.log(actor, STATUS_CHANGE, REFERRAL, id, current.status() + " -> " + target);
        return get(id);
    }

    @Transactional
    public void delete(long id, String actor) {
        Referral current = get(id);
        if (!current.status().isEditable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Удалить можно только направление в статусе «создано»");
        }
        repository.delete(id, current.debtorId());
        audit.log(actor, DELETE, REFERRAL, id, "Удалено ошибочное направление");
    }

    private static String normalizeKey(String key) {
        return key == null || key.isBlank() ? UUID.randomUUID().toString() : key.trim();
    }
}
