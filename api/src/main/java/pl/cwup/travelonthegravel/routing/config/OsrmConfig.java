package pl.cwup.travelonthegravel.routing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;
import pl.cwup.travelonthegravel.routing.client.OsrmApi;

@Configuration
@ImportHttpServices(group = "osrm", types = OsrmApi.class)
public class OsrmConfig {}
