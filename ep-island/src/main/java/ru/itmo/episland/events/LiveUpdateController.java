package ru.itmo.episland.events;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.episland.auth.Access;

@RestController
@RequestMapping("/api/events")
public class LiveUpdateController {
    private final LiveUpdateService service;
    private final Access access;

    public LiveUpdateController(LiveUpdateService service, Access access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest request) {
        access.user(request);
        return service.subscribe();
    }
}
