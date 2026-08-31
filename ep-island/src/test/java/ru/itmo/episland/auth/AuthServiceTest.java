package ru.itmo.episland.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {
    private final AuthService service = new AuthService();

    @Test
    void authenticatesEveryDemoRole() {
        assertThat(service.authenticate("officer", "officer")).get().extracting(SessionUser::role).isEqualTo(Role.OFFICER);
        assertThat(service.authenticate("registrar", "registrar")).get().extracting(SessionUser::role).isEqualTo(Role.REGISTRAR);
        assertThat(service.authenticate("zone", "zone")).get().extracting(SessionUser::role).isEqualTo(Role.ZONE_OPERATOR);
        assertThat(service.authenticate("engineer", "engineer")).get().extracting(SessionUser::role).isEqualTo(Role.ENGINEER);
        assertThat(service.authenticate("analyst", "analyst")).get().extracting(SessionUser::role).isEqualTo(Role.ANALYST);
    }

    @Test
    void rejectsWrongPassword() {
        assertThat(service.authenticate("officer", "wrong")).isEmpty();
    }
}
