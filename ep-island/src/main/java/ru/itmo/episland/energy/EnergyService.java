package ru.itmo.episland.energy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.episland.service.AuditAction;
import ru.itmo.episland.service.AuditService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static ru.itmo.episland.service.AuditAction.ACCOUNTING_DELIVERY;
import static ru.itmo.episland.service.AuditAction.ACCOUNTING_RETRY;
import static ru.itmo.episland.service.AuditAction.ENERGY_CORRECTED;
import static ru.itmo.episland.service.AuditAction.ENERGY_RECORDED;
import static ru.itmo.episland.service.AuditAction.SHEARING_COMPLETED;
import static ru.itmo.episland.service.AuditedEntityType.ENERGY_SHIFT;
import static ru.itmo.episland.service.AuditedEntityType.SHEARING;

@Service
public class EnergyService {
    private static final Logger log = LoggerFactory.getLogger(EnergyService.class);
    private static final Duration CORRECTION_WINDOW = Duration.ofHours(24);
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final EnergyRepository repository;
    private final AccountingGateway accountingGateway;
    private final AuditService audit;
    private final BigDecimal energyCoefficient;

    public EnergyService(EnergyRepository repository,
                         AccountingGateway accountingGateway,
                         AuditService audit,
                         @Value("${ep-island.energy-coefficient}") BigDecimal energyCoefficient) {
        this.repository = repository;
        this.accountingGateway = accountingGateway;
        this.audit = audit;
        this.energyCoefficient = energyCoefficient;
    }

    public List<Shearing> listShearings() {
        return repository.findAllShearings();
    }

    public List<EnergyShift> listShifts() {
        return repository.findAllShifts();
    }

    @Transactional
    public Shearing completeShearing(String residentId, BigDecimal woolKg, String actor) {
        Shearing current = getShearing(residentId);
        if (current.status() == ShearingStatus.COMPLETED
            && Duration.between(current.updatedAt(), Instant.now()).compareTo(CORRECTION_WINDOW) >= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Истёк 24-часовой срок корректировки стрижки");
        }

        BigDecimal predicted = woolKg.multiply(energyCoefficient).setScale(1, RoundingMode.HALF_UP);
        repository.completeShearing(residentId, woolKg, predicted, actor);
        audit.log(actor, SHEARING_COMPLETED, SHEARING, current.id(),
            "Шерсть: " + woolKg + " кг; автопрогноз: " + predicted + " кВт·ч");
        return getShearing(residentId);
    }

    @Transactional
    public EnergyShift createShift(String shiftCode, BigDecimal actualKwh, String actor) {
        long id = repository.insertShift(shiftCode.trim(), actualKwh, actor);
        audit.log(actor, ENERGY_RECORDED, ENERGY_SHIFT, id,
            "Фактическая энергия за смену: " + actualKwh + " кВт·ч");
        return deliver(id, actor, ACCOUNTING_DELIVERY);
    }

    @Transactional
    public EnergyShift correctShift(long id, BigDecimal actualKwh, String actor) {
        EnergyShift current = getShift(id);
        if (Duration.between(current.createdAt(), Instant.now()).compareTo(CORRECTION_WINDOW) >= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Истёк 24-часовой срок корректировки энерговыработки");
        }
        repository.updateShiftForCorrection(id, actualKwh);
        audit.log(actor, ENERGY_CORRECTED, ENERGY_SHIFT, id,
            "Фактическая энергия: " + current.actualKwh() + " -> " + actualKwh);
        return deliver(id, actor, ACCOUNTING_DELIVERY);
    }

    @Transactional
    public EnergyShift retryDelivery(long id, String actor) {
        getShift(id);
        return deliver(id, actor, ACCOUNTING_RETRY);
    }

    @Transactional
    public void retryPendingDeliveries() {
        for (Long id : repository.findPendingShiftIds(50)) {
            deliver(id, SYSTEM_ACTOR, ACCOUNTING_RETRY);
        }
    }

    private EnergyShift deliver(long id, String actor, AuditAction action) {
        EnergyShift shift = getShift(id);
        AccountingDeliveryStatus status;
        try {
            status = accountingGateway.deliver(shift)
                ? AccountingDeliveryStatus.DELIVERED
                : AccountingDeliveryStatus.PENDING;
        } catch (RuntimeException exception) {
            log.warn("Accounting delivery failed for shift {}", id, exception);
            status = AccountingDeliveryStatus.PENDING;
        }
        repository.recordDeliveryAttempt(id, status);
        audit.log(actor, action, ENERGY_SHIFT, id,
            status == AccountingDeliveryStatus.DELIVERED
                ? "Бухгалтерия подтвердила приём"
                : "Бухгалтерия недоступна; оставлено в очереди");
        return getShift(id);
    }

    private Shearing getShearing(String residentId) {
        return repository.findShearing(residentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Задание на стрижку не найдено"));
    }

    private EnergyShift getShift(long id) {
        return repository.findShift(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Смена не найдена"));
    }
}
