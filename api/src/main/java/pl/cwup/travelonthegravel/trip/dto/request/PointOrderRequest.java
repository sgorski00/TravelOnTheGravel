package pl.cwup.travelonthegravel.trip.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PointOrderRequest(@NotNull List<@NotNull Long> pointIds) {}
