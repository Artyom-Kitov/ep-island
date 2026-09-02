package ru.itmo.episland.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
import ru.itmo.episland.energy.EnergyService;
import ru.itmo.episland.energy.EnergyShift;
import ru.itmo.episland.energy.Shearing;
import ru.itmo.episland.events.LiveUpdateScope;
import ru.itmo.episland.events.LiveUpdateService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/energy")
public class EnergyController {
    private final EnergyService service;
    private final Access access;
    private final LiveUpdateService liveUpdates;

    public EnergyController(EnergyService service, Access access, LiveUpdateService liveUpdates) {
        this.service = service;
        this.access = access;
        this.liveUpdates = liveUpdates;
    }

    @GetMapping("/shearings")
    public List<Shearing> shearings(HttpServletRequest request) {
        access.user(request);
        return service.listShearings();
    }

    @PatchMapping("/shearings/{residentId}")
    public Shearing completeShearing(@PathVariable String residentId,
                                     @Valid @RequestBody ShearingRequest body,
                                     HttpServletRequest request) {
        SessionUser user = access.require(request, Role.ENGINEER);
        Shearing shearing = service.completeShearing(residentId, body.woolKg(), user.username());
        liveUpdates.publish(LiveUpdateScope.ENERGY, residentId);
        return shearing;
    }

    @GetMapping("/shifts")
    public List<EnergyShift> shifts(HttpServletRequest request) {
        access.user(request);
        return service.listShifts();
    }

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    public EnergyShift createShift(@Valid @RequestBody EnergyShiftRequest body,
                                   HttpServletRequest request) {
        SessionUser user = access.require(request, Role.ENGINEER);
        EnergyShift shift = service.createShift(body.shiftCode(), body.actualKwh(), user.username());
        liveUpdates.publish(LiveUpdateScope.ENERGY, shift.id());
        return shift;
    }

    @PatchMapping("/shifts/{id}")
    public EnergyShift correctShift(@PathVariable long id,
                                    @Valid @RequestBody EnergyShiftCorrection body,
                                    HttpServletRequest request) {
        SessionUser user = access.require(request, Role.ENGINEER);
        EnergyShift shift = service.correctShift(id, body.actualKwh(), user.username());
        liveUpdates.publish(LiveUpdateScope.ENERGY, id);
        return shift;
    }

    @PostMapping("/shifts/{id}/retry")
    public EnergyShift retry(@PathVariable long id, HttpServletRequest request) {
        SessionUser user = access.require(request, Role.ENGINEER);
        EnergyShift shift = service.retryDelivery(id, user.username());
        liveUpdates.publish(LiveUpdateScope.ENERGY, id);
        return shift;
    }

    public record ShearingRequest(@NotNull @DecimalMin("0.1") BigDecimal woolKg) {
    }

    public record EnergyShiftRequest(@NotBlank String shiftCode,
                                     @NotNull @DecimalMin("0.0") BigDecimal actualKwh) {
    }

    public record EnergyShiftCorrection(@NotNull @DecimalMin("0.0") BigDecimal actualKwh) {
    }
}
