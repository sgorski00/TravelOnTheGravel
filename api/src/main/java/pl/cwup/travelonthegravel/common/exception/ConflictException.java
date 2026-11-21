package pl.cwup.travelonthegravel.common.exception;

import pl.cwup.travelonthegravel.common.model.ErrorCode;

public final class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
