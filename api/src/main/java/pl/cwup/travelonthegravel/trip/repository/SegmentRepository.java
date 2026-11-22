package pl.cwup.travelonthegravel.trip.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.trip.model.Segment;

public interface SegmentRepository extends JpaRepository<Segment, Long> {

    List<Segment> findAllByTripIdOrderByFromPointPosition(Long tripId);

    Optional<Segment> findByIdAndTripId(Long id, Long tripId);
}
