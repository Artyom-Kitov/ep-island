package ru.itmo.episland.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.episland.health.Health;
import ru.itmo.episland.health.HealthService;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final HealthService service;

    public HealthController(HealthService service) {
        this.service = service;
    }

    @GetMapping
    public Health health() {
        return service.health();
    }
}
