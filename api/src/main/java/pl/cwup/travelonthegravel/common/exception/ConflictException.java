package pl.cwup.travelonthegravel.common.exception;

import pl.cwup.travelonthegravel.common.model.ErrorCode;

public final class ConflictException extends ApiException {

    public ConflictException(String message) {
        this(ErrorCode.CONFLICT, message);
    }

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}
