package pl.cwup.travelonthegravel.common.security;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

@Component
public class JwtToUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        final var user = new AuthenticatedUser(userId(jwt), jwt.getClaimAsString("username"));
        return UsernamePasswordAuthenticationToken.authenticated(user, jwt, List.of());
    }

    private static Long userId(Jwt jwt) {
        final var id = jwt.getSubject();
        if (id == null) throw new InvalidBearerTokenException("Token nie zawiera ID użytkownika");
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException ex) {
            throw new InvalidBearerTokenException("Token nie zawiera ID użytkownika");
        }
    }
}
