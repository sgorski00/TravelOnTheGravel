package pl.cwup.travelonthegravel.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
final class AdminUserControllerIT {

    private static final String PASSWORD = "tajnehaslo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    private User admin;
    private User jan;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        admin = new User("admin@example.com", "admin", passwordEncoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);
        jan = new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD));
        jan.setPublic(true);
        jan = userRepository.save(jan);
    }

    @Test
    void blockedUserCannotLoginOrRefreshAndUnblockRestoresLogin() throws Exception {
        final var janRefreshToken = (String) JsonPath.read(login("jan@example.com"), "$.refreshToken");

        asAdmin(post(adminUrl(jan.getId()) + "/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        loginRequest("jan@example.com")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"%s\"}".formatted(janRefreshToken)))
                .andExpect(status().isUnauthorized());

        asAdmin(post(adminUrl(jan.getId()) + "/unblock"))
                .andExpect(jsonPath("$.blocked").value(false));
        loginRequest("jan@example.com").andExpect(status().isOk());
    }

    @Test
    void blockedProfileIsHiddenFromOthers() throws Exception {
        asAdmin(post(adminUrl(jan.getId()) + "/block")).andExpect(status().isOk());

        asAdmin(get("/api/users/search").param("q", "jan"))
                .andExpect(jsonPath("$.content").isEmpty());
        asAdmin(get("/api/users/" + jan.getId())).andExpect(status().isNotFound());
    }

    @Test
    void adminCanPromoteUserToAdmin() throws Exception {
        asAdmin(put(adminUrl(jan.getId()) + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        final String janToken = JsonPath.read(login("jan@example.com"), "$.accessToken");
        assertThat(jwtDecoder.decode(janToken).getClaimAsString("role")).isEqualTo("ADMIN");
        mockMvc.perform(post(adminUrl(admin.getId()) + "/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + janToken))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserHasNoAccessToAdminEndpoints() throws Exception {
        final String janToken = JsonPath.read(login("jan@example.com"), "$.accessToken");

        mockMvc.perform(post(adminUrl(admin.getId()) + "/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + janToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCannotBlockHimselfOrChangeOwnRole() throws Exception {
        asAdmin(post(adminUrl(admin.getId()) + "/block"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
        asAdmin(put(adminUrl(admin.getId()) + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownUserOrInvalidRoleReturnsError() throws Exception {
        asAdmin(post(adminUrl(999_999L) + "/block")).andExpect(status().isNotFound());
        asAdmin(put(adminUrl(jan.getId()) + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"BOSS\"}"))
                .andExpect(status().isBadRequest());
    }

    private static String adminUrl(Long userId) {
        return "/api/admin/users/" + userId;
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        final String token = JsonPath.read(login("admin@example.com"), "$.accessToken");
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String login(String email) throws Exception {
        return loginRequest(email)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private ResultActions loginRequest(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, PASSWORD)));
    }
}
