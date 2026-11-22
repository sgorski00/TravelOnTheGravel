package pl.cwup.travelonthegravel.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.admin.dto.response.AdminUserResponse;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AdminUserResponse block(Long adminId, Long userId) {
        final var user = findOtherUser(adminId, userId);
        user.setBlocked(true);
        refreshTokenRepository.revokeAllByUser(user);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse unblock(Long adminId, Long userId) {
        final var user = findOtherUser(adminId, userId);
        user.setBlocked(false);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse changeRole(Long adminId, Long userId, Role role) {
        final var user = findOtherUser(adminId, userId);
        user.setRole(role);
        refreshTokenRepository.revokeAllByUser(user);
        return AdminUserResponse.from(user);
    }

    private User findOtherUser(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw new BadRequestException(ErrorCode.CANNOT_MODIFY_SELF, "Nie można zmienić własnego konta");
        }
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika"));
    }
}
