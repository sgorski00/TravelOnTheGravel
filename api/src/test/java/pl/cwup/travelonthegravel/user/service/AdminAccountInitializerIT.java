package pl.cwup.travelonthegravel.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "app.admin.email=admin@example.com",
            "app.admin.username=admin",
            "app.admin.password=tajnehaslo-admina"
        })
@AutoConfigureMockMvc
final class AdminAccountInitializerIT {

    @Autowired
    private AdminAccountInitializer adminAccountInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void adminIsCreatedOnStartupOnceAndGetsAdminToken() throws Exception {
        adminAccountInitializer.run(new DefaultApplicationArguments());

        final var admins = userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals("admin@example.com"))
                .toList();
        assertThat(admins)
                .singleElement()
                .satisfies(admin -> assertThat(admin.getRole()).isEqualTo(Role.ADMIN));

        final var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"admin@example.com\", \"password\": \"tajnehaslo-admina\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        final String accessToken = JsonPath.read(response, "$.accessToken");
        assertThat(jwtDecoder.decode(accessToken).getClaimAsString("role")).isEqualTo("ADMIN");
    }
}
