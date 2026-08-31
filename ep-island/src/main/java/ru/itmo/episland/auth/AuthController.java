package ru.itmo.episland.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class AuthController {
    public static final String SESSION_USER = "epIslandUser";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/demo-accounts")
    public List<Map<String, String>> demoAccounts() {
        return authService.demoAccounts();
    }

    @PostMapping("/login")
    public SessionUser login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        SessionUser user = authService.authenticate(request.username(), request.password())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));
        session.setAttribute(SESSION_USER, user);
        return user;
    }

    @GetMapping
    public SessionUser current(HttpSession session) {
        Object user = session.getAttribute(SESSION_USER);
        if (user instanceof SessionUser sessionUser) {
            return sessionUser;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
