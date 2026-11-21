package pl.cwup.travelonthegravel.trip.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import pl.cwup.travelonthegravel.trip.model.Trip;

public record TripResponse(
        Long id, String name, LocalDate startDate, LocalDate endDate, String description, Instant createdAt) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getName(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getDescription(),
                trip.getCreatedAt());
    }
}
