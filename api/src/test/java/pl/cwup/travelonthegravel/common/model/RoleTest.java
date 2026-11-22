package pl.cwup.travelonthegravel.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class RoleTest {

    @Test
    void authorityHasSpringRolePrefix() {
        assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
        assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
