package pl.cwup.travelonthegravel.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
final class AdminUserServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private final User user = user("jan@example.com", "jan");

    @Test
    void blockSetsBlockedAndRevokesSessions() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        final var response = adminUserService.block(ADMIN_ID, USER_ID);

        assertThat(response.blocked()).isTrue();
        assertThat(user.isBlocked()).isTrue();
        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void unblockClearsBlocked() {
        user.setBlocked(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThat(adminUserService.unblock(ADMIN_ID, USER_ID).blocked()).isFalse();
        assertThat(user.isBlocked()).isFalse();
    }

    @Test
    void changeRoleSetsRoleAndRevokesSessions() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        final var response = adminUserService.changeRole(ADMIN_ID, USER_ID, Role.ADMIN);

        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void adminCannotModifyOwnAccount() {
        assertThatThrownBy(() -> adminUserService.block(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.CANNOT_MODIFY_SELF);
        assertThatThrownBy(() -> adminUserService.changeRole(ADMIN_ID, ADMIN_ID, Role.USER))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void unknownUserThrowsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.block(ADMIN_ID, USER_ID)).isInstanceOf(NotFoundException.class);
    }
}
