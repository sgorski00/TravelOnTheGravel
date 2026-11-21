package pl.cwup.travelonthegravel.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.trip.model.Point;

public interface PointRepository extends JpaRepository<Point, Long> {}
