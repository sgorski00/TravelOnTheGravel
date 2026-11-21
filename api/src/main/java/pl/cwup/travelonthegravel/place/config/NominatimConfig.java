package pl.cwup.travelonthegravel.place.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;
import pl.cwup.travelonthegravel.place.client.NominatimApi;

@Configuration
@ImportHttpServices(group = "nominatim", types = NominatimApi.class)
public class NominatimConfig {}
