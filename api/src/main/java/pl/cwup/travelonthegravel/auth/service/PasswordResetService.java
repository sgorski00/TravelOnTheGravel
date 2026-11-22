package pl.cwup.travelonthegravel.auth.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.auth.config.PasswordResetProperties;
import pl.cwup.travelonthegravel.auth.dto.request.PasswordResetConfirmRequest;
import pl.cwup.travelonthegravel.auth.event.PasswordResetRequestedEvent;
import pl.cwup.travelonthegravel.auth.model.PasswordResetToken;
import pl.cwup.travelonthegravel.auth.repository.PasswordResetTokenRepository;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.exception.ForbiddenException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetProperties passwordResetProperties;

    @Transactional
    public void requestReset(String email) {
        userRepository
                .findByEmailIgnoreCase(email)
                .filter(user -> !user.isBlocked())
                .ifPresent(user -> {
                    passwordResetTokenRepository.deleteAllByUser(user);

                    final var token = tokenService.generateRandomToken();
                    final var expiresAt = Instant.now().plus(passwordResetProperties.tokenTtl());
                    passwordResetTokenRepository.save(
                            new PasswordResetToken(user, tokenService.hash(token), expiresAt));

                    eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), token));
                });
    }

    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        final var resetToken = passwordResetTokenRepository
                .findByTokenHash(tokenService.hash(request.token()))
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.INVALID_RESET_TOKEN, "Link do resetu hasła jest nieprawidłowy lub wygasł"));

        final var user = resetToken.getUser();
        if (user.isBlocked()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_BLOCKED, "Konto jest zablokowane");
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        resetToken.setUsed(true);

        // Po zmianie hasła wylogowujemy wszystkie wcześniejsze sesje.
        refreshTokenRepository.revokeAllByUser(user);
    }
}
