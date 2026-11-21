package pl.cwup.travelonthegravel.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pl.cwup.travelonthegravel.common.dto.ApiError;
import pl.cwup.travelonthegravel.common.exception.ApiException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return response(ApiError.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        final var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(ApiError.of(ErrorCode.VALIDATION_ERROR, "Niepoprawne dane w żądaniu", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        return response(ApiError.of(ErrorCode.MALFORMED_REQUEST, "Niepoprawny format żądania"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return response(ApiError.of(ErrorCode.UNAUTHORIZED, "Wymagane zalogowanie"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return response(ApiError.of(ErrorCode.FORBIDDEN, "Brak dostępu do zasobu"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return response(ApiError.of(ErrorCode.NOT_FOUND, "Nie znaleziono zasobu"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("Nieoczekiwany błąd", ex);
        return response(ApiError.of(ErrorCode.INTERNAL_ERROR, "Wystąpił nieoczekiwany błąd"));
    }

    private static ResponseEntity<ApiError> response(ApiError error) {
        return ResponseEntity.status(error.code().getStatus()).body(error);
    }
}
