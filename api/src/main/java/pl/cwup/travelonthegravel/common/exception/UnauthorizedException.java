package pl.cwup.travelonthegravel.common.exception;

import pl.cwup.travelonthegravel.common.model.ErrorCode;

public final class UnauthorizedException extends ApiException {

    public UnauthorizedException(ErrorCode code, String message) {
        super(code, message);
    }
}
