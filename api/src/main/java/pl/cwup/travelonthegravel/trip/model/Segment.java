package pl.cwup.travelonthegravel.trip.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "segments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_point_id", nullable = false)
    private Point fromPoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_point_id", nullable = false)
    private Point toPoint;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false, length = 20)
    private TransportType transportType;

    @Column(name = "distance_meters")
    private Long distanceMeters;

    /** Trasa odcinka jako GeoJSON LineString. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String geometry;

    public Segment(Trip trip, Point fromPoint, Point toPoint, TransportType transportType) {
        this.trip = trip;
        this.fromPoint = fromPoint;
        this.toPoint = toPoint;
        this.transportType = transportType;
    }

    public void setRoute(Long distanceMeters, String geometry) {
        this.distanceMeters = distanceMeters;
        this.geometry = geometry;
    }
}
