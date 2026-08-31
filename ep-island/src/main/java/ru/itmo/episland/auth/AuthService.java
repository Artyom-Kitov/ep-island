package ru.itmo.episland.auth;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private final Map<String, DemoAccount> accounts = new LinkedHashMap<>();

    public AuthService() {
        add("officer", "officer", "Инспектор Пончик", Role.OFFICER);
        add("registrar", "registrar", "Регистратор Знайка", Role.REGISTRAR);
        add("zone", "zone", "Оператор Винтик", Role.ZONE_OPERATOR);
        add("engineer", "engineer", "Инженер Шпунтик", Role.ENGINEER);
        add("analyst", "analyst", "Аналитик Большого Бредлама", Role.ANALYST);
        add("admin", "admin", "Администратор системы", Role.ADMIN);
    }

    private void add(String username, String password, String displayName, Role role) {
        accounts.put(username, new DemoAccount(username, password, displayName, role));
    }

    public Optional<SessionUser> authenticate(String username, String password) {
        DemoAccount account = accounts.get(username == null ? "" : username.trim().toLowerCase());
        if (account == null || !account.password().equals(password)) {
            return Optional.empty();
        }
        return Optional.of(new SessionUser(account.username(), account.displayName(), account.role()));
    }

    public List<Map<String, String>> demoAccounts() {
        return accounts.values().stream()
            .map(account -> Map.of(
                "username", account.username(),
                "password", account.password(),
                "role", account.role().name(),
                "roleTitle", account.role().getTitle(),
                "displayName", account.displayName()))
            .toList();
    }

    private record DemoAccount(String username, String password, String displayName, Role role) {
    }
}
