package pl.cwup.travelonthegravel.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.auth.event.PasswordResetRequestedEvent;
import pl.cwup.travelonthegravel.auth.model.PasswordResetToken;
import pl.cwup.travelonthegravel.auth.repository.PasswordResetTokenRepository;
import pl.cwup.travelonthegravel.auth.service.TokenService;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
final class PasswordResetControllerIT {

    private static final String REQUEST_URL = "/api/auth/password-reset/request";
    private static final String CONFIRM_URL = "/api/auth/password-reset/confirm";
    private static final String OLD_PASSWORD = "stareHaslo1";
    private static final String NEW_PASSWORD = "noweHaslo1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        user = userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(OLD_PASSWORD)));
    }

    @Test
    void requestForExistingEmailSavesTokenAndPublishesEvent() throws Exception {
        requestReset("JAN@example.com")
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertThat(passwordResetTokenRepository.count()).isEqualTo(1);
        assertThat(events.stream(PasswordResetRequestedEvent.class))
                .singleElement()
                .satisfies(event -> assertThat(event.email()).isEqualTo("jan@example.com"));
    }

    @Test
    void requestForUnknownEmailReturnsSameResponseWithoutEmail() throws Exception {
        requestReset("nieistnieje@example.com")
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertThat(passwordResetTokenRepository.count()).isZero();
        assertThat(events.stream(PasswordResetRequestedEvent.class)).isEmpty();
    }

    @Test
    void newRequestInvalidatesPreviousToken() throws Exception {
        requestReset("jan@example.com");
        final var firstToken = lastToken();
        requestReset("jan@example.com");

        confirm(firstToken, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        confirm(lastToken(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void confirmChangesPasswordAndLogsOutPreviousSessions() throws Exception {
        final var refreshToken = refreshTokenOf(login(OLD_PASSWORD).andExpect(status().isOk()));
        requestReset("jan@example.com");

        confirm(lastToken(), NEW_PASSWORD).andExpect(status().isOk());

        login(OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
        postJson("/api/auth/refresh", "{\"refreshToken\": \"%s\"}".formatted(refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usedTokenReturns400() throws Exception {
        requestReset("jan@example.com");
        final var token = lastToken();
        confirm(token, NEW_PASSWORD).andExpect(status().isOk());

        confirm(token, "jeszczeInne1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void expiredTokenReturns400() throws Exception {
        final var token = tokenService.generateRandomToken();
        passwordResetTokenRepository.save(new PasswordResetToken(
                user, tokenService.hash(token), Instant.now().minusSeconds(60)));

        confirm(token, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void unknownTokenReturns400() throws Exception {
        confirm("nieznany-token", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void invalidNewPasswordReturns400WithFieldErrors() throws Exception {
        postJson(CONFIRM_URL, """
                        {"token": "abc", "password": "krotkie", "passwordConfirmation": "inne"}
                        """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private ResultActions requestReset(String email) throws Exception {
        return postJson(REQUEST_URL, "{\"email\": \"%s\"}".formatted(email));
    }

    private ResultActions confirm(String token, String password) throws Exception {
        return postJson(CONFIRM_URL, """
                {"token": "%s", "password": "%s", "passwordConfirmation": "%s"}
                """.formatted(token, password, password));
    }

    private ResultActions login(String password) throws Exception {
        return postJson("/api/auth/login", """
                {"email": "jan@example.com", "password": "%s"}
                """.formatted(password));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String lastToken() {
        return events.stream(PasswordResetRequestedEvent.class)
                .reduce((first, second) -> second)
                .orElseThrow()
                .token();
    }

    private static String refreshTokenOf(ResultActions loginResult) throws Exception {
        return JsonPath.read(loginResult.andReturn().getResponse().getContentAsString(), "$.refreshToken");
    }
}
