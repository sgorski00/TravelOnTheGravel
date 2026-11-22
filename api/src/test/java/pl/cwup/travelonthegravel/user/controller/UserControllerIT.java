package pl.cwup.travelonthegravel.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class UserControllerIT {

    private static final String ME_URL = "/api/users/me";
    private static final String PASSWORD = "stareHaslo1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD)));
        userRepository.save(new User("ola@example.com", "ola", passwordEncoder.encode(PASSWORD)));

        final var tokens =
                login("jan@example.com", PASSWORD).andReturn().getResponse().getContentAsString();
        accessToken = JsonPath.read(tokens, "$.accessToken");
        refreshToken = JsonPath.read(tokens, "$.refreshToken");
    }

    @Test
    void getProfileReturnsOwnData() throws Exception {
        perform(get(ME_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jan@example.com"))
                .andExpect(jsonPath("$.username").value("jan"))
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    void getProfileWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ME_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void isPublicChangeIsSavedAndReturned() throws Exception {
        perform(patch(ME_URL).contentType(MediaType.APPLICATION_JSON).content("{\"isPublic\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(true));

        perform(get(ME_URL)).andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    void updateWithTakenEmailOrUsernameReturns409() throws Exception {
        perform(patch(ME_URL).contentType(MediaType.APPLICATION_JSON).content("{\"email\": \"OLA@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        perform(patch(ME_URL).contentType(MediaType.APPLICATION_JSON).content("{\"username\": \"Ola\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void updateWithInvalidDataReturns400() throws Exception {
        perform(patch(ME_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"zly-email\", \"username\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void roleCannotBeChangedByProfileUpdate() throws Exception {
        perform(patch(ME_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\", \"isPublic\": true}"))
                .andExpect(status().isOk());

        assertThat(userRepository
                        .findByEmailIgnoreCase("jan@example.com")
                        .orElseThrow()
                        .getRole())
                .isEqualTo(Role.USER);
    }

    @Test
    void changePasswordWithWrongCurrentPasswordReturns400() throws Exception {
        changePassword("zleHaslo1", "noweHaslo1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void afterPasswordChangeOldPasswordFailsNewWorksAndSessionsAreRevoked() throws Exception {
        changePassword(PASSWORD, "noweHaslo1").andExpect(status().isNoContent());

        login("jan@example.com", PASSWORD).andExpect(status().isUnauthorized());
        login("jan@example.com", "noweHaslo1").andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordWithDifferentConfirmationReturns400OnConfirmationField() throws Exception {
        perform(put(ME_URL + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"currentPassword": "%s", "newPassword": "noweHaslo1", "newPasswordConfirmation": "inne"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPasswordConfirmation"));
    }

    private ResultActions changePassword(String currentPassword, String newPassword) throws Exception {
        return perform(put(ME_URL + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {"currentPassword": "%s", "newPassword": "%s", "newPasswordConfirmation": "%s"}
                """.formatted(currentPassword, newPassword, newPassword)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    }
}
