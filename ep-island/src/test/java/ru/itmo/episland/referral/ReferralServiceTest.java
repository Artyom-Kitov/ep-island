package ru.itmo.episland.referral;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.episland.service.AuditService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {
    @Mock
    private ReferralRepository repository;
    @Mock
    private RegistryGateway registryGateway;
    @Mock
    private AuditService audit;
    @InjectMocks
    private ReferralService service;

    @Test
    void returnsExistingReferralForRepeatedIdempotencyKey() {
        Referral existing = referral(17L, ReferralStatus.CREATED);
        when(repository.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        Referral result = service.create(data(), "same-key", "officer");

        assertThat(result).isSameAs(existing);
        verify(repository, never()).insertDebtor(data(), "officer");
    }

    @Test
    void createsDebtorReferralAndAuditRecordInOneFlow() {
        Referral created = referral(23L, ReferralStatus.CREATED);
        CreateReferralData data = data();
        when(repository.findByIdempotencyKey("new-key")).thenReturn(Optional.empty());
        when(repository.insertDebtor(data, "admin")).thenReturn(11L);
        when(repository.insertReferral(11L, "new-key", "admin")).thenReturn(23L);
        when(repository.findById(23L)).thenReturn(Optional.of(created));

        Referral result = service.create(data, "new-key", "admin");

        assertThat(result).isEqualTo(created);
        verify(audit).log("admin", ru.itmo.episland.service.AuditAction.CREATE,
            ru.itmo.episland.service.AuditedEntityType.REFERRAL, 23L,
            "Создано электронное направление");
    }

    @Test
    void handsCreatedReferralToConvoy() {
        Referral created = referral(31L, ReferralStatus.CREATED);
        Referral handed = referral(31L, ReferralStatus.HANDED_TO_CONVOY);
        when(repository.findById(31L)).thenReturn(Optional.of(created), Optional.of(handed));

        Referral result = service.updateStatus(31L, ReferralStatus.HANDED_TO_CONVOY, "officer");

        assertThat(result.status()).isEqualTo(ReferralStatus.HANDED_TO_CONVOY);
        verify(repository).updateStatus(31L, ReferralStatus.HANDED_TO_CONVOY);
    }

    private static CreateReferralData data() {
        return new CreateReferralData(
            "Тестовый Коротышка",
            LocalDate.of(1990, 1, 1),
            new BigDecimal("12000.00"),
            "Задолженность",
            "Акт"
        );
    }

    private static Referral referral(long id, ReferralStatus status) {
        return new Referral(
            id,
            8L,
            "Тестовый Коротышка",
            LocalDate.of(1990, 1, 1),
            new BigDecimal("12000.00"),
            "Задолженность",
            "Акт",
            status,
            "officer",
            Instant.parse("2026-08-30T12:00:00Z")
        );
    }
}
