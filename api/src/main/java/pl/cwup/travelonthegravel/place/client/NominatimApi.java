package pl.cwup.travelonthegravel.place.client;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimPlace;

@HttpExchange
public interface NominatimApi {

    @GetExchange("/search?format=jsonv2&addressdetails=1")
    List<NominatimPlace> search(@RequestParam("q") String query, @RequestParam("limit") int limit);

    @GetExchange("/reverse?format=jsonv2&zoom=3")
    NominatimPlace reverse(@RequestParam("lat") double latitude, @RequestParam("lon") double longitude);
}
