package pl.cwup.travelonthegravel.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.trip.model.Segment;

public interface SegmentRepository extends JpaRepository<Segment, Long> {}
