package pl.cwup.travelonthegravel.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "TravelOnTheGravel API",
                        version = "${app.version}",
                        description = "API aplikacji TravelOnTheGravel",
                        contact =
                                @Contact(
                                        name = "Zespół TravelOnTheGravel",
                                        url = "https://github.com/sgorski00/TravelOnTheGravel")))
public class OpenApiConfig {}
