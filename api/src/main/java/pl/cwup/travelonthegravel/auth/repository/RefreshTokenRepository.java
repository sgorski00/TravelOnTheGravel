package pl.cwup.travelonthegravel.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.auth.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
