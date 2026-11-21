package pl.cwup.travelonthegravel.user.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.cwup.travelonthegravel.TestData.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
final class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void emailMustBeUniqueIgnoringCase() {
        userRepository.saveAndFlush(user("Jan@Example.com", "jan"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("jan@example.com", "jan2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usernameMustBeUniqueIgnoringCase() {
        userRepository.saveAndFlush(user("jan@example.com", "Jan"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("jan2@example.com", "jan")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
