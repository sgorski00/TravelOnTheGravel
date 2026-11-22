package pl.cwup.travelonthegravel.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.config.AdminProperties;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

final class AdminAccountInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ApplicationArguments args = mock(ApplicationArguments.class);

    @Test
    void createsAdminWhenNotExists() {
        when(passwordEncoder.encode("tajnehaslo")).thenReturn("hash");

        initializer(new AdminProperties("admin@example.com", "admin", "tajnehaslo"))
                .run(args);

        final var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void doesNothingWhenAdminAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        initializer(new AdminProperties("admin@example.com", "admin", "tajnehaslo"))
                .run(args);

        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenNotConfigured() {
        initializer(new AdminProperties("", "admin", "")).run(args);

        verify(userRepository, never()).existsByEmailIgnoreCase(any());
        verify(userRepository, never()).save(any());
    }

    private AdminAccountInitializer initializer(AdminProperties properties) {
        return new AdminAccountInitializer(properties, userRepository, passwordEncoder);
    }
}
