package pl.cwup.travelonthegravel.trip.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import pl.cwup.travelonthegravel.common.model.Coordinates;

@Entity
@Table(name = "points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {

    public static final int NAME_MAX_LENGTH = 255;
    public static final int COUNTRY_CODE_LENGTH = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Setter
    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** Kod kraju ISO 3166-1 alpha-2, np. "PL". */
    @Column(name = "country_code", length = COUNTRY_CODE_LENGTH)
    private String countryCode;

    @Setter
    @Column(nullable = false)
    private int position;

    public Point(Trip trip, String name, double latitude, double longitude, String countryCode, int position) {
        this.trip = trip;
        this.name = name;
        this.position = position;
        setLocation(latitude, longitude, countryCode);
    }

    public void setLocation(double latitude, double longitude, String countryCode) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.countryCode = countryCode;
    }

    public boolean isAt(double latitude, double longitude) {
        return this.latitude == latitude && this.longitude == longitude;
    }

    public Coordinates coordinates() {
        return new Coordinates(latitude, longitude);
    }
}
