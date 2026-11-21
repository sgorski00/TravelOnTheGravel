package pl.cwup.travelonthegravel.auth.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.auth.model.RefreshToken;
import pl.cwup.travelonthegravel.user.model.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);
}
