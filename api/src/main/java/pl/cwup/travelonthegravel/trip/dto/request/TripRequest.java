package pl.cwup.travelonthegravel.trip.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import pl.cwup.travelonthegravel.common.validation.DateRange;
import pl.cwup.travelonthegravel.common.validation.ValidDateRange;
import pl.cwup.travelonthegravel.trip.model.Trip;

@ValidDateRange
public record TripRequest(
        @NotBlank @Size(max = Trip.NAME_MAX_LENGTH) String name,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Size(max = Trip.DESCRIPTION_MAX_LENGTH) String description)
        implements DateRange {}
