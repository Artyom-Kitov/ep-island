package ru.itmo.episland.auth;

public enum Role {
    OFFICER("Офицер полиции"),
    REGISTRAR("Оператор регистрации"),
    ZONE_OPERATOR("Оператор развлекательного комплекса"),
    ENGINEER("Инженер-энергетик"),
    ANALYST("Аналитик Большого Бредлама"),
    ADMIN("Администратор");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
