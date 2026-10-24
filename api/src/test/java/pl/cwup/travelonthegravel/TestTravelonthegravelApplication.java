package pl.cwup.travelonthegravel;

import org.springframework.boot.SpringApplication;

public class TestTravelonthegravelApplication {

	static void main(String[] args) {
		SpringApplication.from(TravelonthegravelApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
