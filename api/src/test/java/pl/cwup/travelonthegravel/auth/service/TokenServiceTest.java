package pl.cwup.travelonthegravel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.user;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.cwup.travelonthegravel.auth.config.JwtConfig;
import pl.cwup.travelonthegravel.auth.config.JwtProperties;
import pl.cwup.travelonthegravel.common.model.Role;

final class TokenServiceTest {

    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            "test-only-secret-at-least-32-characters-long", Duration.ofMinutes(15), Duration.ofDays(30));

    private final JwtConfig jwtConfig = new JwtConfig();
    private final TokenService tokenService = new TokenService(jwtConfig.jwtEncoder(JWT_PROPERTIES), JWT_PROPERTIES);

    @Test
    void accessTokenContainsUserIdUsernameAndExpiration() {
        final var user = user("jan@example.com", "jan");
        ReflectionTestUtils.setField(user, "id", 7L);

        final var jwt = jwtConfig.jwtDecoder(JWT_PROPERTIES).decode(tokenService.createAccessToken(user));

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("jan");
        assertThat(jwt.getExpiresAt())
                .isBetween(
                        Instant.now().plus(Duration.ofMinutes(14)),
                        Instant.now().plus(Duration.ofMinutes(16)));
    }

    @Test
    void generatedTokensAreRandom() {
        assertThat(tokenService.generateRandomToken()).isNotEqualTo(tokenService.generateRandomToken());
    }

    @Test
    void hashIsStableAndDifferentFromToken() {
        final var token = tokenService.generateRandomToken();

        assertThat(tokenService.hash(token)).isEqualTo(tokenService.hash(token));
        assertThat(tokenService.hash(token)).isNotEqualTo(token).hasSize(64);
    }

    @Test
    void accessTokenContainsRole() {
        final var admin = user("admin@example.com", "admin");
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setRole(Role.ADMIN);

        final var jwt = jwtConfig.jwtDecoder(JWT_PROPERTIES).decode(tokenService.createAccessToken(admin));

        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
    }
}
