package pl.cwup.travelonthegravel.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.common.model.Role;

final class UserTest {

    @Test
    void constructorSetsFieldsAndUserIsPrivateByDefault() {
        final var user = new User("jan@example.com", "jan", "hash");

        assertThat(user.getEmail()).isEqualTo("jan@example.com");
        assertThat(user.getUsername()).isEqualTo("jan");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.isPublic()).isFalse();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isBlocked()).isFalse();
    }
}
