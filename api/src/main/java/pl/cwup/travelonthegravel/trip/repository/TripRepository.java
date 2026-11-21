package pl.cwup.travelonthegravel.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.trip.model.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {}
