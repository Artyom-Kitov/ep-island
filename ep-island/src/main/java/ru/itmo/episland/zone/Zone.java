package ru.itmo.episland.zone;

import java.math.BigDecimal;

public record Zone(
    long id,
    String name,
    int capacity,
    int occupied,
    BigDecimal transformationCoefficient,
    BigDecimal predictedHours,
    boolean active,
    BigDecimal score
) {
    public Zone withScore(BigDecimal newScore) {
        return new Zone(id, name, capacity, occupied, transformationCoefficient,
            predictedHours, active, newScore);
    }

    public int loadPercent() {
        return capacity == 0 ? 0 : (int) Math.round(occupied * 100.0 / capacity);
    }

    public boolean hasFreePlaces() {
        return active && occupied < capacity;
    }
}
