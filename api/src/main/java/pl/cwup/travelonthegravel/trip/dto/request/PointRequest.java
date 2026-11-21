package pl.cwup.travelonthegravel.trip.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.cwup.travelonthegravel.trip.model.Point;

public record PointRequest(
        @NotBlank @Size(max = Point.NAME_MAX_LENGTH) String name,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {}
