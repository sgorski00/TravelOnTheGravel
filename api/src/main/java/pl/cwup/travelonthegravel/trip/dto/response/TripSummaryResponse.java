package pl.cwup.travelonthegravel.trip.dto.response;

import java.time.LocalDate;
import pl.cwup.travelonthegravel.trip.model.Trip;

public record TripSummaryResponse(Long id, String name, LocalDate startDate, LocalDate endDate) {

    public static TripSummaryResponse from(Trip trip) {
        return new TripSummaryResponse(trip.getId(), trip.getName(), trip.getStartDate(), trip.getEndDate());
    }
}
