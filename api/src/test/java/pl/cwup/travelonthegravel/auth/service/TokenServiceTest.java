package pl.cwup.travelonthegravel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TokenServiceTest {

    private final TokenService tokenService = new TokenService(null, null);

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
}
