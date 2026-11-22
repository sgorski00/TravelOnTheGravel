package pl.cwup.travelonthegravel.common.exception;

import pl.cwup.travelonthegravel.common.model.ErrorCode;

public final class ForbiddenException extends ApiException {

    public ForbiddenException(ErrorCode code, String message) {
        super(code, message);
    }
}
