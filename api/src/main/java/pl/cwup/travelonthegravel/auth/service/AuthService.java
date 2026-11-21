package pl.cwup.travelonthegravel.auth.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.auth.config.JwtProperties;
import pl.cwup.travelonthegravel.auth.dto.request.LoginRequest;
import pl.cwup.travelonthegravel.auth.dto.request.RegisterRequest;
import pl.cwup.travelonthegravel.auth.dto.response.TokenResponse;
import pl.cwup.travelonthegravel.auth.model.RefreshToken;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.ConflictException;
import pl.cwup.travelonthegravel.common.exception.UnauthorizedException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.user.dto.response.UserResponse;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException(ErrorCode.EMAIL_TAKEN, "E-mail jest już zajęty");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException(ErrorCode.USERNAME_TAKEN, "Nazwa użytkownika jest już zajęta");
        }

        final var user = new User(request.email(), request.username(), passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        final var user = userRepository
                .findByEmailIgnoreCase(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() ->
                        new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, "Nieprawidłowy e-mail lub hasło"));

        final var refreshToken = tokenService.generateRandomToken();
        final var expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(user, tokenService.hash(refreshToken), expiresAt));

        return tokens(user, refreshToken);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        final var token = refreshTokenRepository
                .findByTokenHash(tokenService.hash(refreshToken))
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token jest nieprawidłowy lub wygasł"));

        return tokens(token.getUser(), refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository
                .findByTokenHash(tokenService.hash(refreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private TokenResponse tokens(User user, String refreshToken) {
        return TokenResponse.bearer(
                tokenService.createAccessToken(user),
                refreshToken,
                jwtProperties.accessTokenTtl().toSeconds());
    }
}
