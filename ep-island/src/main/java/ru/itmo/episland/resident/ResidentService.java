package ru.itmo.episland.resident;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.episland.service.AuditService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static ru.itmo.episland.service.AuditAction.CORRECT;
import static ru.itmo.episland.service.AuditAction.DELETE_DUPLICATE;
import static ru.itmo.episland.service.AuditAction.REGISTER;
import static ru.itmo.episland.service.AuditedEntityType.RESIDENT;

@Service
public class ResidentService {
    private static final Duration CORRECTION_WINDOW = Duration.ofHours(24);

    private final ResidentRepository repository;
    private final ResidentIdGenerator idGenerator;
    private final AuditService audit;

    public ResidentService(ResidentRepository repository, ResidentIdGenerator idGenerator, AuditService audit) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.audit = audit;
    }

    public List<Resident> list(ResidentStatus status) {
        return status == null ? repository.findAll() : repository.findAllByStatus(status);
    }

    public Resident get(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Коротышка не найдена"));
    }

    @Transactional
    public Resident register(long referralId, String actor) {
        if (!repository.existsReferralReadyForArrival(referralId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Направление не передано конвою или не найдено");
        }
        repository.findByReferralId(referralId).ifPresent(duplicate -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Прибывший по этому направлению уже зарегистрирован: " + duplicate.id());
        });

        String id = idGenerator.next();
        repository.insert(id, referralId, actor);
        audit.log(actor, REGISTER, RESIDENT, id, "Прибытие по направлению " + referralId);
        return get(id);
    }

    @Transactional
    public Resident update(String id, UpdateResidentData data, String actor) {
        Resident resident = get(id);
        assertEditable(resident);
        repository.updateProfile(resident, data, actor);
        audit.log(actor, CORRECT, RESIDENT, id, "Скорректирована карточка прибывшего");
        return get(id);
    }

    @Transactional
    public void delete(String id, String actor) {
        Resident resident = get(id);
        assertEditable(resident);
        repository.delete(id);
        audit.log(actor, DELETE_DUPLICATE, RESIDENT, id, "Удалена подтверждённая дублирующая запись");
    }

    private static void assertEditable(Resident resident) {
        if (!resident.status().isRegistrationEditable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Корректировать или удалять можно только запись в статусе «прибыл»");
        }
        if (Duration.between(resident.arrivedAt(), Instant.now()).compareTo(CORRECTION_WINDOW) >= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Истёк 24-часовой срок корректировки");
        }
    }
}
