package ru.itmo.episland.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.episland.auth.Access;
import ru.itmo.episland.auth.Role;
import ru.itmo.episland.auth.SessionUser;
import ru.itmo.episland.zone.Zone;
import ru.itmo.episland.zone.ZoneAssignment;
import ru.itmo.episland.zone.ZoneRecommendation;
import ru.itmo.episland.zone.ZoneService;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {
    private final ZoneService service;
    private final Access access;

    public ZoneController(ZoneService service, Access access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<Zone> zones(HttpServletRequest request) {
        access.user(request);
        return service.listZones();
    }

    @GetMapping("/assignments")
    public List<ZoneAssignment> assignments(HttpServletRequest request) {
        access.user(request);
        return service.listAssignments();
    }

    @GetMapping("/recommendation/{residentId}")
    public ZoneRecommendation recommendation(@PathVariable String residentId, HttpServletRequest request) {
        access.require(request, Role.ZONE_OPERATOR);
        return service.recommend(residentId);
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneAssignment assign(@Valid @RequestBody AssignmentRequest body, HttpServletRequest request) {
        SessionUser user = access.require(request, Role.ZONE_OPERATOR);
        return service.assign(body.residentId(), body.zoneId(), user.username());
    }

    @PatchMapping("/assignments/{id}/transformation")
    public ZoneAssignment transform(@PathVariable long id,
                                    @Valid @RequestBody TransformationRequest body,
                                    HttpServletRequest request) {
        SessionUser user = access.require(request, Role.ZONE_OPERATOR);
        return service.recordTransformation(id, body.percent(), user.username());
    }

    public record AssignmentRequest(@NotBlank String residentId, @NotNull Long zoneId) {
    }

    public record TransformationRequest(@Min(0) @Max(100) int percent) {
    }
}
