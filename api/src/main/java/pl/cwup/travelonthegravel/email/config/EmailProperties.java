package pl.cwup.travelonthegravel.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record EmailProperties(String from, String resetPasswordUrl) {}
