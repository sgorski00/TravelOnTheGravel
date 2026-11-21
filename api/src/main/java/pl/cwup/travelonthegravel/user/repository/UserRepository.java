package pl.cwup.travelonthegravel.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.cwup.travelonthegravel.user.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);
}
