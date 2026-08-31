package ru.itmo.episland.zone;

public enum TransformationStage {
    INITIAL,
    INTERMEDIATE,
    COMPLETED;

    public static TransformationStage fromPercent(int percent) {
        if (percent == 100) {
            return COMPLETED;
        }
        return percent < 34 ? INITIAL : INTERMEDIATE;
    }
}
