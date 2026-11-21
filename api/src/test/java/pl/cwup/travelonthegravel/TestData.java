package pl.cwup.travelonthegravel;

import java.time.LocalDate;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Segment;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.user.model.User;

/** Metody pomocnicze do tworzenia obiektów w testach. */
public final class TestData {

    private TestData() {}

    public static User user(String email, String username) {
        return new User(email, username, "hash");
    }

    public static Trip trip(User user, String name) {
        return new Trip(user, name, LocalDate.of(2026, 7, 1), null, null);
    }

    public static Point point(Trip trip, String name, int position) {
        return new Point(trip, name, 52.23, 21.01, "PL", position);
    }

    public static Segment segment(Trip trip, Point from, Point to, TransportType transportType) {
        return new Segment(trip, from, to, transportType);
    }
}
