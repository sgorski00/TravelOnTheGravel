package pl.cwup.travelonthegravel.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.config.AdminProperties;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminProperties.isConfigured()) {
            log.info("Brak ADMIN_EMAIL / ADMIN_PASSWORD, konto administratora nie jest tworzone");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(adminProperties.email())) {
            return;
        }
        final var admin = new User(
                adminProperties.email(),
                adminProperties.username(),
                passwordEncoder.encode(adminProperties.password()));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Utworzono konto administratora {}", adminProperties.email());
    }
}
