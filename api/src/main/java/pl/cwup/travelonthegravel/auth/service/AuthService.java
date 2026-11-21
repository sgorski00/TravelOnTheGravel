package pl.cwup.travelonthegravel.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.auth.dto.RegisterRequest;
import pl.cwup.travelonthegravel.common.exception.ConflictException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.user.dto.UserResponse;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
