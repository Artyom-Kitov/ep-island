package ru.itmo.episland.zone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.episland.resident.ResidentStatus;
import ru.itmo.episland.service.AuditService;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.itmo.episland.service.AuditAction.TRANSFORMATION_RECORDED;
import static ru.itmo.episland.service.AuditedEntityType.ZONE_ASSIGNMENT;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {
    @Mock
    private ZoneRepository repository;
    @Mock
    private AuditService audit;
    @InjectMocks
    private ZoneService service;

    @Test
    void completingTransformationCreatesShearingTaskAndHistory() {
        ZoneAssignment before = assignment(45);
        ZoneAssignment after = assignment(100);
        when(repository.findActiveAssignment(7L)).thenReturn(Optional.of(before));
        when(repository.findAssignment(7L)).thenReturn(Optional.of(after));

        service.recordTransformation(7L, 100, "zone");

        verify(repository).updateTransformation(7L, 100, "zone");
        verify(repository).recordTransformationHistory(7L, 45, 100, "zone");
        verify(repository).updateResidentStatus("EPIABC2345", ResidentStatus.TRANSFORMED, "zone");
        verify(repository).createShearingTask("EPIABC2345", "zone");
        verify(audit).log("zone", TRANSFORMATION_RECORDED, ZONE_ASSIGNMENT, 7L,
            "Трансформация: 45% -> 100%");
    }

    @Test
    void transformationProgressCannotGoBackwards() {
        when(repository.findActiveAssignment(7L)).thenReturn(Optional.of(assignment(60)));

        assertThatThrownBy(() -> service.recordTransformation(7L, 40, "zone"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("не может уменьшаться");

        verify(repository, never()).updateTransformation(7L, 40, "zone");
    }

    private static ZoneAssignment assignment(int percent) {
        Instant timestamp = Instant.parse("2026-08-31T05:00:00Z");
        return new ZoneAssignment(7L, "EPIABC2345", "Тестовый Коротышка", 2L,
            "Восток", percent, timestamp, timestamp);
    }
}
