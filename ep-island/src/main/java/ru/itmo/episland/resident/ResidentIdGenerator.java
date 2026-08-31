package ru.itmo.episland.resident;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ResidentIdGenerator {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final ResidentRepository repository;

    public ResidentIdGenerator(ResidentRepository repository) {
        this.repository = repository;
    }

    public String next() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder candidate = new StringBuilder("EPI");
            for (int i = 0; i < 7; i++) {
                candidate.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            if (!repository.existsById(candidate.toString())) {
                return candidate.toString();
            }
        }
        throw new IllegalStateException("Не удалось сформировать уникальный ID коротышки");
    }
}
