package pl.cwup.travelonthegravel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.cwup.travelonthegravel.auth.config.JwtProperties;
import pl.cwup.travelonthegravel.auth.dto.request.LoginRequest;
import pl.cwup.travelonthegravel.auth.dto.request.RegisterRequest;
import pl.cwup.travelonthegravel.auth.model.RefreshToken;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.ConflictException;
import pl.cwup.travelonthegravel.common.exception.UnauthorizedException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
final class AuthServiceTest {

    private static final JwtProperties JWT_PROPERTIES =
            new JwtProperties("secret", Duration.ofMinutes(15), Duration.ofDays(30));

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(userRepository, refreshTokenRepository, passwordEncoder, tokenService, JWT_PROPERTIES);
    }

    @Test
    void registerSavesUserWithHashedPassword() {
        when(passwordEncoder.encode("tajnehaslo")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = authService.register(registerRequest());

        final var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(response.email()).isEqualTo("jan@example.com");
        assertThat(response.username()).isEqualTo("jan");
    }

    @Test
    void registerWithTakenEmailThrowsConflict() {
        when(userRepository.existsByEmailIgnoreCase("jan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EMAIL_TAKEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerWithTakenUsernameThrowsConflict() {
        when(userRepository.existsByUsernameIgnoreCase("jan")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.USERNAME_TAKEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokensAndSavesRefreshTokenHash() {
        final var user = user("jan@example.com", "jan");
        when(userRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("tajnehaslo", "hash")).thenReturn(true);
        when(tokenService.generateRandomToken()).thenReturn("refresh");
        when(tokenService.hash("refresh")).thenReturn("refresh-hash");
        when(tokenService.createAccessToken(user)).thenReturn("access");

        final var response = authService.login(new LoginRequest("jan@example.com", "tajnehaslo"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        final var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo("refresh-hash");
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(29)));
    }

    @Test
    void loginWithUnknownEmailThrowsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("jan@example.com", "tajnehaslo")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginWithWrongPasswordThrowsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCase("jan@example.com"))
                .thenReturn(Optional.of(user("jan@example.com", "jan")));
        when(passwordEncoder.matches("zlehaslo", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jan@example.com", "zlehaslo")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshWithValidTokenReturnsNewAccessToken() {
        final var user = user("jan@example.com", "jan");
        mockRefreshToken(new RefreshToken(user, "refresh-hash", Instant.now().plusSeconds(60)));
        when(tokenService.createAccessToken(user)).thenReturn("new-access");

        final var response = authService.refresh("refresh");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void refreshWithUnknownTokenThrows() {
        when(tokenService.hash("refresh")).thenReturn("refresh-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("refresh"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refreshWithExpiredTokenThrows() {
        mockRefreshToken(new RefreshToken(
                user("jan@example.com", "jan"), "refresh-hash", Instant.now().minusSeconds(60)));

        assertThatThrownBy(() -> authService.refresh("refresh"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refreshWithRevokedTokenThrows() {
        final var token = new RefreshToken(
                user("jan@example.com", "jan"), "refresh-hash", Instant.now().plusSeconds(60));
        token.setRevoked(true);
        mockRefreshToken(token);

        assertThatThrownBy(() -> authService.refresh("refresh"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void logoutRevokesRefreshToken() {
        final var token = new RefreshToken(
                user("jan@example.com", "jan"), "refresh-hash", Instant.now().plusSeconds(60));
        mockRefreshToken(token);

        authService.logout("refresh");

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void logoutWithUnknownTokenDoesNothing() {
        when(tokenService.hash("refresh")).thenReturn("refresh-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-hash")).thenReturn(Optional.empty());

        authService.logout("refresh");

        verify(refreshTokenRepository, never()).save(any());
    }

    private void mockRefreshToken(RefreshToken token) {
        when(tokenService.hash("refresh")).thenReturn("refresh-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-hash")).thenReturn(Optional.of(token));
    }

    private static RegisterRequest registerRequest() {
        return new RegisterRequest("jan@example.com", "jan", "tajnehaslo", "tajnehaslo");
    }
}
