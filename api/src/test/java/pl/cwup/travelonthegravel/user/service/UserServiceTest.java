package pl.cwup.travelonthegravel.user.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.exception.ConflictException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.user.dto.request.ChangePasswordRequest;
import pl.cwup.travelonthegravel.user.dto.request.UpdateProfileRequest;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
final class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final User user = user("jan@example.com", "jan");

    @Test
    void getProfileReturnsUserData() {
        mockUser();

        final var profile = userService.getProfile(USER_ID);

        assertThat(profile.email()).isEqualTo("jan@example.com");
        assertThat(profile.username()).isEqualTo("jan");
        assertThat(profile.isPublic()).isFalse();
    }

    @Test
    void getProfileOfUnknownUserThrowsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(USER_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateProfileChangesOnlySentFields() {
        mockUser();

        final var profile = userService.updateProfile(USER_ID, new UpdateProfileRequest(null, null, true));

        assertThat(profile.isPublic()).isTrue();
        assertThat(profile.email()).isEqualTo("jan@example.com");
        assertThat(profile.username()).isEqualTo("jan");
    }

    @Test
    void updateProfileChangesEmailAndUsername() {
        mockUser();

        final var profile =
                userService.updateProfile(USER_ID, new UpdateProfileRequest("janek", "janek@example.com", null));

        assertThat(profile.email()).isEqualTo("janek@example.com");
        assertThat(profile.username()).isEqualTo("janek");
    }

    @Test
    void updateProfileToOwnEmailWithDifferentCaseIsNotConflict() {
        mockUser();

        final var profile = userService.updateProfile(USER_ID, new UpdateProfileRequest(null, "JAN@example.com", null));

        assertThat(profile.email()).isEqualTo("JAN@example.com");
        verify(userRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void updateProfileWithTakenEmailThrowsConflict() {
        mockUser();
        when(userRepository.existsByEmailIgnoreCase("ola@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                        userService.updateProfile(USER_ID, new UpdateProfileRequest(null, "ola@example.com", null)))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EMAIL_TAKEN);
        assertThat(user.getEmail()).isEqualTo("jan@example.com");
    }

    @Test
    void updateProfileWithTakenUsernameThrowsConflict() {
        mockUser();
        when(userRepository.existsByUsernameIgnoreCase("ola")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(USER_ID, new UpdateProfileRequest("ola", null, null)))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.USERNAME_TAKEN);
        assertThat(user.getUsername()).isEqualTo("jan");
    }

    @Test
    void changePasswordSavesNewHashAndRevokesSessions() {
        mockUser();
        when(passwordEncoder.matches("stareHaslo1", "hash")).thenReturn(true);
        when(passwordEncoder.encode("noweHaslo1")).thenReturn("new-hash");

        userService.changePassword(USER_ID, new ChangePasswordRequest("stareHaslo1", "noweHaslo1", "noweHaslo1"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void changePasswordWithWrongCurrentPasswordThrows() {
        mockUser();
        when(passwordEncoder.matches("zleHaslo1", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(
                        USER_ID, new ChangePasswordRequest("zleHaslo1", "noweHaslo1", "noweHaslo1")))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
    }

    private void mockUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }
}
