package ru.itmo.episland.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.episland.auth.Access;
import ru.itmo.episland.auth.Role;
import ru.itmo.episland.auth.SessionUser;
import ru.itmo.episland.events.LiveUpdateScope;
import ru.itmo.episland.events.LiveUpdateService;
import ru.itmo.episland.referral.CreateReferralData;
import ru.itmo.episland.referral.Referral;
import ru.itmo.episland.referral.ReferralService;
import ru.itmo.episland.referral.ReferralStatus;
import ru.itmo.episland.referral.RegistryLookup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {
    private final ReferralService service;
    private final Access access;
    private final LiveUpdateService liveUpdates;

    public ReferralController(ReferralService service, Access access, LiveUpdateService liveUpdates) {
        this.service = service;
        this.access = access;
        this.liveUpdates = liveUpdates;
    }

    @GetMapping
    public List<Referral> list(HttpServletRequest request) {
        access.user(request);
        return service.list();
    }

    @GetMapping("/search")
    public List<Referral> search(@RequestParam(defaultValue = "") String fullName,
                                 @RequestParam(defaultValue = "12") int limit,
                                 HttpServletRequest request) {
        access.user(request);
        return service.searchPendingArrival(fullName, limit);
    }

    @GetMapping("/{id}")
    public Referral get(@PathVariable long id, HttpServletRequest request) {
        access.user(request);
        return service.get(id);
    }

    @GetMapping("/registry-search")
    public RegistryLookup registrySearch(@RequestParam String fullName, HttpServletRequest request) {
        access.require(request, Role.OFFICER);
        return service.searchRegistry(fullName);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Referral create(@Valid @RequestBody CreateReferralRequest body,
                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                           HttpServletRequest request) {
        SessionUser user = access.require(request, Role.OFFICER);
        Referral created = service.create(body.toData(), idempotencyKey, user.username());
        liveUpdates.publish(LiveUpdateScope.REFERRALS, created.id());
        return created;
    }

    @PatchMapping("/{id}/status")
    public Referral updateStatus(@PathVariable long id, @Valid @RequestBody StatusRequest body,
                                 HttpServletRequest request) {
        SessionUser user = access.require(request, Role.OFFICER);
        Referral updated = service.updateStatus(id, body.status(), user.username());
        liveUpdates.publish(LiveUpdateScope.REFERRALS, id);
        return updated;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, HttpServletRequest request) {
        SessionUser user = access.require(request, Role.OFFICER);
        service.delete(id, user.username());
        liveUpdates.publish(LiveUpdateScope.REFERRALS, id);
    }

    public record CreateReferralRequest(
        @NotBlank @Size(max = 200) String fullName,
        LocalDate birthDate,
        @NotNull @DecimalMin("0.0") BigDecimal debtAmount,
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 2000) String documents
    ) {
        CreateReferralData toData() {
            return new CreateReferralData(fullName, birthDate, debtAmount, reason, documents);
        }
    }

    public record StatusRequest(@NotNull ReferralStatus status) {
    }
}
