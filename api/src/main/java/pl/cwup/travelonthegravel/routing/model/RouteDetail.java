package pl.cwup.travelonthegravel.routing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// FULL: każdy zakręt drogi (tysiące punktów), SIMPLIFIED: uproszczona linia (kilkadziesiąt punktów, ten sam dystans).
@Getter
@RequiredArgsConstructor
public enum RouteDetail {
    FULL("full"),
    SIMPLIFIED("simplified");

    private final String osrmValue;
}
