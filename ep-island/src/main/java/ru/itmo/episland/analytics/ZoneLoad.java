package ru.itmo.episland.analytics;

public record ZoneLoad(long id, String name, int capacity, int occupied, int loadPercent) {
}
