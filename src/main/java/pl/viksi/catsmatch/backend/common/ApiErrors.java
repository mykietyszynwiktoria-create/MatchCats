package pl.viksi.catsmatch.backend.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    public record ErrorBody(int status, String code, String message, Map<String, String> fields) {}
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> application(ApiException ex) {
        return response(ex.status, ex.code, ex.getMessage(), Map.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Check the submitted fields", fields);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorBody> malformed(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request body or parameter", Map.of());
    }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorBody> authentication(AuthenticationException ex) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password", Map.of());
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> forbidden(AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", Map.of());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> conflict(DataIntegrityViolationException ex) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "Conflicting or duplicated data", Map.of());
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorBody> stale(OptimisticLockingFailureException ex) {
        return response(HttpStatus.CONFLICT, "STALE_VERSION", "Refresh the cat profile before saving", Map.of());
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorBody> upload(MaxUploadSizeExceededException ex) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "The file exceeds the upload limit", Map.of());
    }
    private ResponseEntity<ErrorBody> response(HttpStatus status, String code, String message, Map<String,String> fields) {
        return ResponseEntity.status(status).body(new ErrorBody(status.value(), code, message, fields));
    }
}
