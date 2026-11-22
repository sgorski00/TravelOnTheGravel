package pl.cwup.travelonthegravel.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class JwtAuthenticationIT {

    private static final String PROTECTED_URL = "/api/protected";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void validTokenPassesSecurity() throws Exception {
        mockMvc.perform(get(PROTECTED_URL)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token(Instant.now().plusSeconds(60)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void wrongTokenReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer("to.nie.jest.token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token(Instant.now().minusSeconds(60)))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenWithoutRoleReturns401() throws Exception {
        final var claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        final var token = jwtEncoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenWithoutUserIdReturns401() throws Exception {
        final var claims = JwtClaimsSet.builder()
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        final var token = jwtEncoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String token(Instant expiresAt) {
        final var claims = JwtClaimsSet.builder()
                .subject("1")
                .claim("role", "USER")
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
