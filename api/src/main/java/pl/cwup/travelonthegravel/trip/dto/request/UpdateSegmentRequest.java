package pl.cwup.travelonthegravel.trip.dto.request;

import jakarta.validation.constraints.NotNull;
import pl.cwup.travelonthegravel.trip.model.TransportType;

public record UpdateSegmentRequest(@NotNull TransportType transportType) {}
