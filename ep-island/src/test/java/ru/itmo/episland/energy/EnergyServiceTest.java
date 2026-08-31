package ru.itmo.episland.energy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.episland.service.AuditService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyServiceTest {
    @Mock
    private EnergyRepository repository;
    @Mock
    private AccountingGateway accountingGateway;
    @Mock
    private AuditService audit;

    private EnergyService service;

    @BeforeEach
    void setUp() {
        service = new EnergyService(repository, accountingGateway, audit, new BigDecimal("14.2"));
    }

    @Test
    void predictedEnergyIsCalculatedAutomaticallyFromManualWoolMeasurement() {
        Shearing waiting = shearing(ShearingStatus.WAITING, null, null);
        Shearing completed = shearing(ShearingStatus.COMPLETED,
            new BigDecimal("7.2"), new BigDecimal("102.2"));
        when(repository.findShearing("EPIABC2345"))
            .thenReturn(Optional.of(waiting), Optional.of(completed));

        Shearing result = service.completeShearing("EPIABC2345", new BigDecimal("7.2"), "engineer");

        assertThat(result.predictedEnergyKwh()).isEqualByComparingTo("102.2");
        verify(repository).completeShearing("EPIABC2345", new BigDecimal("7.2"),
            new BigDecimal("102.2"), "engineer");
    }

    @Test
    void actualEnergyIsDeliveredAutomaticallyAfterEngineerRecordsShift() {
        EnergyShift pending = shift(AccountingDeliveryStatus.PENDING, 0);
        EnergyShift delivered = shift(AccountingDeliveryStatus.DELIVERED, 1);
        when(repository.insertShift("SHIFT-A", new BigDecimal("125.4"), "engineer")).thenReturn(9L);
        when(repository.findShift(9L)).thenReturn(Optional.of(pending), Optional.of(delivered));
        when(accountingGateway.deliver(pending)).thenReturn(true);

        EnergyShift result = service.createShift("SHIFT-A", new BigDecimal("125.4"), "engineer");

        assertThat(result.deliveryStatus()).isEqualTo(AccountingDeliveryStatus.DELIVERED);
        verify(repository).recordDeliveryAttempt(9L, AccountingDeliveryStatus.DELIVERED);
    }

    private static Shearing shearing(ShearingStatus status, BigDecimal wool, BigDecimal predicted) {
        return new Shearing(3L, "EPIABC2345", "Тестовый Коротышка", wool, predicted,
            status, Instant.now());
    }

    private static EnergyShift shift(AccountingDeliveryStatus status, int attempts) {
        Instant now = Instant.parse("2026-08-31T05:00:00Z");
        return new EnergyShift(9L, "SHIFT-A", new BigDecimal("125.4"), status, attempts,
            "engineer", now, now);
    }
}
