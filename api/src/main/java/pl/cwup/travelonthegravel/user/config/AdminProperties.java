package pl.cwup.travelonthegravel.user.config;

import jakarta.validation.constraints.Email;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Zły ADMIN_EMAIL zatrzymuje start aplikacji, zamiast tworzyć konto, na które nie da się zalogować.
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @Email(message = "ADMIN_EMAIL musi być poprawnym adresem e-mail")
        String email,

        String username,
        String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
