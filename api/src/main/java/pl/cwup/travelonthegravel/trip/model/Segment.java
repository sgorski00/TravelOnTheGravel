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
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private GeoJsonLineString geometry;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geometry_simplified", columnDefinition = "jsonb")
    private GeoJsonLineString geometrySimplified;

    @Column(name = "is_fallback", nullable = false)
    private boolean isFallback = false;

    public Segment(Trip trip, Point fromPoint, Point toPoint, TransportType transportType) {
        this.trip = trip;
        this.fromPoint = fromPoint;
        this.toPoint = toPoint;
        this.transportType = transportType;
    }

    public void setRoute(
            long distanceMeters, GeoJsonLineString geometry, GeoJsonLineString geometrySimplified, boolean isFallback) {
        this.distanceMeters = distanceMeters;
        this.geometry = geometry;
        this.geometrySimplified = geometrySimplified;
        this.isFallback = isFallback;
    }

    public boolean connects(Point from, Point to) {
        return fromPoint.getId().equals(from.getId()) && toPoint.getId().equals(to.getId());
    }
}
