package pl.cwup.travelonthegravel.trip.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.trip.model.Point;

public interface PointRepository extends JpaRepository<Point, Long> {

    List<Point> findAllByTripIdOrderByPosition(Long tripId);

    Optional<Point> findByIdAndTripId(Long id, Long tripId);

    int countByTripId(Long tripId);
}
