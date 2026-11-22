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

import com.jayway.jsonpath.JsonPath;
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
import pl.cwup.travelonthegravel.auth.config.JwtProperties;
import pl.cwup.travelonthegravel.auth.repository.RefreshTokenRepository;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class AuthControllerIT {

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String REFRESH_URL = "/api/auth/refresh";
    private static final String LOGOUT_URL = "/api/auth/logout";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

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
        assertThat(saved.getRole()).isEqualTo(Role.USER);
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

    @Test
    void loginWithCorrectDataReturnsBothTokens() throws Exception {
        registerJan();

        login("JAN@example.com", "tajnehaslo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(jwtProperties.accessTokenTtl().toSeconds()));

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void loginWithWrongPasswordOrEmailReturnsSame401() throws Exception {
        registerJan();

        login("jan@example.com", "zlehaslo")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Nieprawidłowy e-mail lub hasło"));
        login("nieistnieje@example.com", "tajnehaslo")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Nieprawidłowy e-mail lub hasło"));
    }

    @Test
    void loginWithInvalidEmailReturns400() throws Exception {
        login("zly-email", "tajnehaslo")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void refreshReturnsNewAccessToken() throws Exception {
        registerJan();
        final var refreshToken = refreshTokenOf(login("jan@example.com", "tajnehaslo"));

        postRefreshToken(REFRESH_URL, refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    void refreshWithUnknownTokenReturns401() throws Exception {
        postRefreshToken(REFRESH_URL, "nieznany-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refreshTokenDoesNotWorkAfterLogout() throws Exception {
        registerJan();
        final var refreshToken = refreshTokenOf(login("jan@example.com", "tajnehaslo"));

        postRefreshToken(LOGOUT_URL, refreshToken).andExpect(status().isNoContent());

        postRefreshToken(REFRESH_URL, refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private void registerJan() throws Exception {
        register("jan@example.com", "jan", "tajnehaslo", "tajnehaslo").andExpect(status().isCreated());
    }

    private ResultActions login(String email, String password) throws Exception {
        final var body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(
                post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions postRefreshToken(String url, String refreshToken) throws Exception {
        final var body = """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String refreshTokenOf(ResultActions loginResult) throws Exception {
        return JsonPath.read(loginResult.andReturn().getResponse().getContentAsString(), "$.refreshToken");
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
