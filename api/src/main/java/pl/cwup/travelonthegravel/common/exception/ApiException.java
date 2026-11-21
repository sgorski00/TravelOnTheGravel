package pl.cwup.travelonthegravel.common.exception;

import lombok.Getter;
import pl.cwup.travelonthegravel.common.model.ErrorCode;

@Getter
public abstract class ApiException extends RuntimeException {

    private final ErrorCode code;

    protected ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
