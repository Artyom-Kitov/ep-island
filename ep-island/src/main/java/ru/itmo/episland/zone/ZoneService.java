package ru.itmo.episland.zone;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.episland.resident.ResidentStatus;
import ru.itmo.episland.service.AuditService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import static ru.itmo.episland.service.AuditAction.ASSIGN;
import static ru.itmo.episland.service.AuditAction.TRANSFORMATION_RECORDED;
import static ru.itmo.episland.service.AuditedEntityType.ZONE_ASSIGNMENT;

@Service
public class ZoneService {
    private static final BigDecimal LOAD_WEIGHT = new BigDecimal("0.5");
    private static final BigDecimal COEFFICIENT_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal TIME_WEIGHT = new BigDecimal("0.2");

    private final ZoneRepository repository;
    private final AuditService audit;

    public ZoneService(ZoneRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    public List<Zone> listZones() {
        List<Zone> zones = repository.findAllWithOccupancy();
        BigDecimal maxCoefficient = zones.stream().map(Zone::transformationCoefficient)
            .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        BigDecimal maxHours = zones.stream().map(Zone::predictedHours)
            .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);

        return zones.stream()
            .map(zone -> zone.withScore(score(zone, maxCoefficient, maxHours)))
            .toList();
    }

    public List<ZoneAssignment> listAssignments() {
        return repository.findActiveAssignments();
    }

    public ZoneRecommendation recommend(String residentId) {
        ensureResidentEligible(residentId);
        Zone zone = listZones().stream()
            .filter(Zone::hasFreePlaces)
            .max(Comparator.comparing(Zone::score))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Все зоны заполнены"));
        return new ZoneRecommendation(residentId, zone,
            "Минимальная загрузка с учётом коэффициента и времени трансформации");
    }

    @Transactional
    public ZoneAssignment assign(String residentId, long zoneId, String actor) {
        ensureResidentEligible(residentId);
        Zone zone = listZones().stream()
            .filter(item -> item.id() == zoneId)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Зона не найдена"));
        if (!zone.hasFreePlaces()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Выбранная зона недоступна или заполнена");
        }

        repository.closeActiveAssignment(residentId);
        long assignmentId = repository.insertAssignment(residentId, zoneId, actor);
        repository.updateResidentStatus(residentId, ResidentStatus.ASSIGNED, actor);
        audit.log(actor, ASSIGN, ZONE_ASSIGNMENT, assignmentId,
            "Коротышка " + residentId + " назначена в зону " + zone.name());
        return getAssignment(assignmentId);
    }

    @Transactional
    public ZoneAssignment recordTransformation(long assignmentId, int percent, String actor) {
        ZoneAssignment assignment = getActiveAssignment(assignmentId);
        validateTransformationProgress(assignment.transformationPercent(), percent);

        repository.updateTransformation(assignmentId, percent, actor);
        repository.recordTransformationHistory(
            assignmentId, assignment.transformationPercent(), percent, actor);

        if (percent == 100) {
            repository.updateResidentStatus(assignment.residentId(), ResidentStatus.TRANSFORMED, actor);
            repository.createShearingTask(assignment.residentId(), actor);
        }

        audit.log(actor, TRANSFORMATION_RECORDED, ZONE_ASSIGNMENT, assignmentId,
            "Трансформация: " + assignment.transformationPercent() + "% -> " + percent + "%");
        return getAssignment(assignmentId);
    }

    private ZoneAssignment getAssignment(long id) {
        return repository.findAssignment(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Назначение не найдено"));
    }

    private ZoneAssignment getActiveAssignment(long id) {
        return repository.findActiveAssignment(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Активное назначение не найдено"));
    }

    private void ensureResidentEligible(String residentId) {
        if (!repository.isResidentEligible(residentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Коротышка не найдена или уже завершила трансформацию");
        }
    }

    private static void validateTransformationProgress(int current, int target) {
        if (current == 100) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Трансформация уже завершена");
        }
        if (target < current) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Процент трансформации не может уменьшаться");
        }
    }

    private static BigDecimal score(Zone zone, BigDecimal maxCoefficient, BigDecimal maxHours) {
        BigDecimal load = BigDecimal.valueOf(zone.occupied())
            .divide(BigDecimal.valueOf(zone.capacity()), 4, RoundingMode.HALF_UP);
        BigDecimal coefficient = zone.transformationCoefficient()
            .divide(maxCoefficient, 4, RoundingMode.HALF_UP);
        BigDecimal time = BigDecimal.ONE.subtract(zone.predictedHours()
            .divide(maxHours, 4, RoundingMode.HALF_UP));
        return BigDecimal.ONE.subtract(load).multiply(LOAD_WEIGHT)
            .add(coefficient.multiply(COEFFICIENT_WEIGHT))
            .add(time.multiply(TIME_WEIGHT))
            .setScale(3, RoundingMode.HALF_UP);
    }
}
