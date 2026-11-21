package pl.cwup.travelonthegravel.common.dto.response;

import java.time.Instant;
import java.util.List;
import pl.cwup.travelonthegravel.common.model.ErrorCode;

public record ApiError(int status, ErrorCode code, String message, List<FieldError> fieldErrors, Instant timestamp) {

    public record FieldError(String field, String message) {}

    public static ApiError of(ErrorCode code, String message) {
        return of(code, message, List.of());
    }

    public static ApiError of(ErrorCode code, String message, List<FieldError> fieldErrors) {
        return new ApiError(code.getStatus().value(), code, message, fieldErrors, Instant.now());
    }
}
