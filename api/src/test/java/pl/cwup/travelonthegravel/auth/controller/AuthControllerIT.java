package pl.cwup.travelonthegravel.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.cwup.travelonthegravel.TestData.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class AuthControllerIT {

    private static final String REGISTER_URL = "/api/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerCreatesUserWithHashedPassword() throws Exception {
        register("jan@example.com", "jan", "tajnehaslo", "tajnehaslo")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value("jan@example.com"))
                .andExpect(jsonPath("$.username").value("jan"))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("tajnehaslo"))));

        final var saved =
                userRepository.findByEmailIgnoreCase("jan@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("tajnehaslo");
        assertThat(passwordEncoder.matches("tajnehaslo", saved.getPasswordHash()))
                .isTrue();
    }

    @Test
    void registerWithInvalidDataReturns400WithFieldErrors() throws Exception {
        register("zly-email", "", "krotkie", "inne")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(containsInAnyOrder("email", "username", "password", "passwordConfirmation")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'username')].message")
                        .value("nie może być puste"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void registerWithTakenEmailReturns409() throws Exception {
        userRepository.save(user("jan@example.com", "jan"));

        register("JAN@example.com", "inny", "tajnehaslo", "tajnehaslo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void registerWithTakenUsernameReturns409() throws Exception {
        userRepository.save(user("jan@example.com", "jan"));

        register("inny@example.com", "JAN", "tajnehaslo", "tajnehaslo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    private ResultActions register(String email, String username, String password, String passwordConfirmation)
            throws Exception {
        final var body = """
                {"email": "%s", "username": "%s", "password": "%s", "passwordConfirmation": "%s"}
                """.formatted(email, username, password, passwordConfirmation);
        return mockMvc.perform(
                post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
