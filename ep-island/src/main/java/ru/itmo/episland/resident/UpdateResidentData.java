package ru.itmo.episland.resident;

import java.time.LocalDate;

public record UpdateResidentData(String fullName, LocalDate birthDate) {
}
