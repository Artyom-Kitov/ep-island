package ru.itmo.episland.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Component
public class Access {
    public SessionUser user(HttpServletRequest request) {
        Object value = request.getSession().getAttribute(AuthController.SESSION_USER);
        if (value instanceof SessionUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
    }

    public SessionUser require(HttpServletRequest request, Role... roles) {
        SessionUser user = user(request);
        if (user.role() == Role.ADMIN || Arrays.asList(roles).contains(user.role())) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Роль «" + user.role().getTitle() + "» не имеет доступа к этой операции");
    }
}
