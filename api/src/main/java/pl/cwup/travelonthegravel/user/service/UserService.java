package pl.cwup.travelonthegravel.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.exception.ConflictException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.user.dto.request.ChangePasswordRequest;
import pl.cwup.travelonthegravel.user.dto.request.UpdateProfileRequest;
import pl.cwup.travelonthegravel.user.dto.response.UserResponse;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        final var user = findUser(userId);

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.email())) {
                throw new ConflictException(ErrorCode.EMAIL_TAKEN, "E-mail jest już zajęty");
            }
        }
        if (request.username() != null && !request.username().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(request.username())) {
                throw new ConflictException(ErrorCode.USERNAME_TAKEN, "Nazwa użytkownika jest już zajęta");
            }
        }

        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.username() != null) {
            user.setUsername(request.username());
        }
        if (request.isPublic() != null) {
            user.setPublic(request.isPublic());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        final var user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(ErrorCode.INVALID_CURRENT_PASSWORD, "Obecne hasło jest nieprawidłowe");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Po zmianie hasła wylogowujemy wszystkie sesje (refresh tokeny).
        refreshTokenRepository.revokeAllByUser(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika"));
    }
}
