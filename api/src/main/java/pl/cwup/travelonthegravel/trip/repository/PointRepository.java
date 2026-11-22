package pl.cwup.travelonthegravel.trip.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.cwup.travelonthegravel.trip.model.Point;

public interface PointRepository extends JpaRepository<Point, Long> {

    List<Point> findAllByTripIdOrderByPosition(Long tripId);

    Optional<Point> findByIdAndTripId(Long id, Long tripId);

    int countByTripId(Long tripId);

    List<Point> findAllByTripUserIdOrderByTripIdAscPositionAsc(Long userId);

    @Query("""
            select distinct p.countryCode from Point p
            where p.trip.user.id = :userId and p.countryCode is not null
            order by p.countryCode""")
    List<String> findVisitedCountryCodes(Long userId);
}
