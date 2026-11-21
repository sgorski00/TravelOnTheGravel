package pl.cwup.travelonthegravel.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.auth.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
