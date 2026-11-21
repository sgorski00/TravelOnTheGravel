package pl.cwup.travelonthegravel.trip.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.trip.model.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByUserIdOrderByStartDateDesc(Long userId);

    Optional<Trip> findByIdAndUserId(Long id, Long userId);
}
