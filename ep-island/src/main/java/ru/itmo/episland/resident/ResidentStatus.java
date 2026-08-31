package ru.itmo.episland.resident;

public enum ResidentStatus {
    ARRIVED,
    ASSIGNED,
    TRANSFORMED;

    public boolean isRegistrationEditable() {
        return this == ARRIVED;
    }
}
