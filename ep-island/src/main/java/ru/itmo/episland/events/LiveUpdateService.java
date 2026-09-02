package ru.itmo.episland.events;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveUpdateService {
    private static final long NO_TIMEOUT = 0L;

    private final CopyOnWriteArrayList<SseEmitter> subscribers = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        subscribers.add(emitter);
        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> subscribers.remove(emitter));
        emitter.onError(error -> subscribers.remove(emitter));
        // The initial event makes a reconnected client reload the current DB state.
        send(emitter, SseEmitter.event().data(LiveUpdate.of(LiveUpdateScope.ALL, null)));
        return emitter;
    }

    public void publish(LiveUpdateScope scope, Object entityId) {
        LiveUpdate update = LiveUpdate.of(scope, entityId);
        subscribers.forEach(emitter -> send(emitter, SseEmitter.event().data(update)));
    }

    @Scheduled(fixedRateString = "${ep-island.live-updates-heartbeat-ms:25000}")
    public void heartbeat() {
        subscribers.forEach(emitter -> send(emitter, SseEmitter.event().comment("keepalive")));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            subscribers.remove(emitter);
            emitter.complete();
        }
    }
}
