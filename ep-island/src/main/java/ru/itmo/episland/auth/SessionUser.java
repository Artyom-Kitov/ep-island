package ru.itmo.episland.auth;

import java.io.Serializable;

public record SessionUser(String username, String displayName, Role role) implements Serializable {
}
