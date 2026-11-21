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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.cwup.travelonthegravel.auth.config.PasswordResetProperties;
import pl.cwup.travelonthegravel.auth.dto.request.PasswordResetConfirmRequest;
import pl.cwup.travelonthegravel.auth.event.PasswordResetRequestedEvent;
import pl.cwup.travelonthegravel.auth.model.PasswordResetToken;
import pl.cwup.travelonthegravel.auth.repository.PasswordResetTokenRepository;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
final class PasswordResetServiceTest {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final User user = user("jan@example.com", "jan");

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                tokenService,
                passwordEncoder,
                eventPublisher,
                new PasswordResetProperties(TOKEN_TTL));
    }

    @Test
    void requestResetDeletesOldTokensSavesNewOneAndPublishesEvent() {
        when(userRepository.findByEmailIgnoreCase("JAN@example.com")).thenReturn(Optional.of(user));
        when(tokenService.generateRandomToken()).thenReturn("token");
        when(tokenService.hash("token")).thenReturn("token-hash");

        passwordResetService.requestReset("JAN@example.com");

        verify(passwordResetTokenRepository).deleteAllByUser(user);
        final var saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo("token-hash");
        assertThat(saved.getValue().getExpiresAt())
                .isBetween(
                        Instant.now().plus(TOKEN_TTL).minusSeconds(5),
                        Instant.now().plus(TOKEN_TTL));
        verify(eventPublisher).publishEvent(new PasswordResetRequestedEvent("jan@example.com", "token"));
    }

    @Test
    void requestResetForUnknownEmailDoesNothing() {
        when(userRepository.findByEmailIgnoreCase("nikt@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("nikt@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void confirmResetChangesPasswordMarksTokenUsedAndRevokesSessions() {
        final var resetToken =
                new PasswordResetToken(user, "token-hash", Instant.now().plusSeconds(60));
        mockResetToken(resetToken);
        when(passwordEncoder.encode("noweHaslo1")).thenReturn("new-hash");

        passwordResetService.confirmReset(confirmRequest());

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(resetToken.isUsed()).isTrue();
        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void confirmResetWithUnknownTokenThrows() {
        when(tokenService.hash("token")).thenReturn("token-hash");
        when(passwordResetTokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.empty());

        assertInvalidToken();
    }

    @Test
    void confirmResetWithExpiredTokenThrows() {
        mockResetToken(new PasswordResetToken(user, "token-hash", Instant.now().minusSeconds(60)));

        assertInvalidToken();
    }

    @Test
    void confirmResetWithUsedTokenThrows() {
        final var resetToken =
                new PasswordResetToken(user, "token-hash", Instant.now().plusSeconds(60));
        resetToken.setUsed(true);
        mockResetToken(resetToken);

        assertInvalidToken();
    }

    private void assertInvalidToken() {
        assertThatThrownBy(() -> passwordResetService.confirmReset(confirmRequest()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_RESET_TOKEN);
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
    }

    private void mockResetToken(PasswordResetToken token) {
        when(tokenService.hash("token")).thenReturn("token-hash");
        when(passwordResetTokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.of(token));
    }

    private static PasswordResetConfirmRequest confirmRequest() {
        return new PasswordResetConfirmRequest("token", "noweHaslo1", "noweHaslo1");
    }
}
