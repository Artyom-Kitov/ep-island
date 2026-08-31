package ru.itmo.episland.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
            .body(body(exception.getReason() == null ? "Ошибка запроса" : exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Проверьте заполнение формы");
        return ResponseEntity.badRequest().body(body(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException exception) {
        log.debug("Request body cannot be parsed", exception);
        return ResponseEntity.badRequest().body(body("Проверьте формат и значения полей формы"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> unsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        log.debug("Unsupported request content type", exception);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(body("Сервер ожидает данные формы в формате JSON"));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> requestParameter(Exception exception) {
        log.debug("Invalid request parameter", exception);
        return ResponseEntity.badRequest().body(body("Проверьте параметры запроса"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> resourceNotFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body("Ресурс не найден"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException exception) {
        log.warn("Database constraint violation", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(body("Операция нарушает целостность данных или создаёт дубликат"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
        String errorId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Unhandled API error {}", errorId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(body("Внутренняя ошибка приложения. Код: " + errorId));
    }

    private Map<String, Object> body(String message) {
        return Map.of("message", message, "timestamp", Instant.now().toString());
    }
}
