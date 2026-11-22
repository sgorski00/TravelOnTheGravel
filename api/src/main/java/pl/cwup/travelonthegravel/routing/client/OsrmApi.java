package pl.cwup.travelonthegravel.routing.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import pl.cwup.travelonthegravel.routing.dto.osrm.OsrmRouteResponse;

@HttpExchange
public interface OsrmApi {

    // format coordinates: "lon,lat;lon,lat"
    @GetExchange("/route/v1/driving/{coordinates}?geometries=geojson")
    OsrmRouteResponse route(@PathVariable String coordinates, @RequestParam("overview") String overview);
}
