package ru.itmo.episland.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.episland.auth.Access;
import ru.itmo.episland.auth.Role;
import ru.itmo.episland.auth.SessionUser;
import ru.itmo.episland.resident.Resident;
import ru.itmo.episland.resident.ResidentService;
import ru.itmo.episland.resident.ResidentStatus;
import ru.itmo.episland.resident.UpdateResidentData;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {
    private final ResidentService service;
    private final Access access;

    public ResidentController(ResidentService service, Access access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<Resident> list(@RequestParam(required = false) ResidentStatus status,
                               HttpServletRequest request) {
        access.user(request);
        return service.list(status);
    }

    @GetMapping("/{id}")
    public Resident get(@PathVariable String id, HttpServletRequest request) {
        access.user(request);
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resident register(@Valid @RequestBody RegisterResidentRequest body, HttpServletRequest request) {
        SessionUser user = access.require(request, Role.REGISTRAR);
        return service.register(body.referralId(), user.username());
    }

    @PatchMapping("/{id}")
    public Resident update(@PathVariable String id, @Valid @RequestBody UpdateResidentRequest body,
                           HttpServletRequest request) {
        SessionUser user = access.require(request, Role.REGISTRAR);
        return service.update(id, new UpdateResidentData(body.fullName(), body.birthDate()), user.username());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, HttpServletRequest request) {
        SessionUser user = access.require(request, Role.REGISTRAR);
        service.delete(id, user.username());
    }

    public record RegisterResidentRequest(@NotNull Long referralId) {
    }

    public record UpdateResidentRequest(String fullName, LocalDate birthDate) {
    }
}
