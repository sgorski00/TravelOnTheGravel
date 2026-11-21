package pl.cwup.travelonthegravel.common.exception;

import pl.cwup.travelonthegravel.common.model.ErrorCode;

public final class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message);
    }
}
