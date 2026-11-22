package pl.cwup.travelonthegravel.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import pl.cwup.travelonthegravel.common.model.Role;

final class JwtToUserConverterTest {

    private final JwtToUserConverter converter = new JwtToUserConverter();

    @Test
    void principalContainsUserIdAndUsername() {
        final var authentication = converter.convert(jwt("42"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(42L, "jan", Role.USER));
    }

    @Test
    void tokenWithoutSubjectIsRejected() {
        assertThatThrownBy(() -> converter.convert(jwt(null))).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void tokenWithNonNumericSubjectIsRejected() {
        assertThatThrownBy(() -> converter.convert(jwt("abc"))).isInstanceOf(InvalidBearerTokenException.class);
    }

    private static Jwt jwt(String subject) {
        final var builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("username", "jan")
                .claim("role", "USER");
        if (subject != null) {
            builder.subject(subject);
        }
        return builder.build();
    }

    @Test
    void adminRoleGivesAdminAuthority() {
        final var jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("1")
                .claim("role", "ADMIN")
                .build();

        final var authentication = converter.convert(jwt);

        assertThat(((AuthenticatedUser) authentication.getPrincipal()).role()).isEqualTo(Role.ADMIN);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void tokenWithoutRoleIsRejected() {
        final var jwt =
                Jwt.withTokenValue("token").header("alg", "HS256").subject("1").build();

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void tokenWithUnknownRoleIsRejected() {
        final var jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("1")
                .claim("role", "BOSS")
                .build();

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }
}
