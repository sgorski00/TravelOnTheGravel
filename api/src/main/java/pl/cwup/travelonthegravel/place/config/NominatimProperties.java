package pl.cwup.travelonthegravel.place.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// nomitamin allows only 1 req/sec
@ConfigurationProperties(prefix = "app.nominatim")
public record NominatimProperties(Duration minInterval) {}
