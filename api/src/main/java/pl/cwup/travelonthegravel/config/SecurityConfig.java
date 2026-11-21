package pl.cwup.travelonthegravel.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/api/auth/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        // 401/403 errors
        final AuthenticationEntryPoint entryPoint =
                (request, response, ex) -> resolver.resolveException(request, response, null, ex);
        final AccessDeniedHandler accessDeniedHandler =
                (request, response, ex) -> resolver.resolveException(request, response, null, ex);

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ENDPOINTS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(Customizer.withDefaults()).authenticationEntryPoint(entryPoint))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
