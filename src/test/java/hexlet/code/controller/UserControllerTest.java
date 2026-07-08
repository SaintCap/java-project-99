package hexlet.code.controller;

import hexlet.code.dto.UserCreateDTO;
import hexlet.code.dto.UserUpdateDTO;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@google.com");
        testUser.setPassword(passwordEncoder.encode("secret"));
        userRepository.save(testUser);
    }

    @Test
    void testIndex() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("john@google.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void testShow() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().intValue()))
                .andExpect(jsonPath("$.email").value("john@google.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void testShowNotFound() throws Exception {
        mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreate() throws Exception {
        var data = new UserCreateDTO();
        data.setEmail("jack@google.com");
        data.setFirstName("Jack");
        data.setLastName("Jons");
        data.setPassword("some-password");

        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("jack@google.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var user = userRepository.findByEmail("jack@google.com").orElseThrow();
        assertThat(user.getFirstName()).isEqualTo("Jack");
        // password must be hashed, not stored in plain text
        assertThat(user.getPassword()).isNotEqualTo("some-password");
        assertThat(passwordEncoder.matches("some-password", user.getPassword())).isTrue();
    }

    @Test
    void testCreateWithInvalidEmail() throws Exception {
        var data = Map.of("email", "not-an-email", "password", "some-password");

        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithShortPassword() throws Exception {
        var data = Map.of("email", "jack@google.com", "password", "ab");

        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdate() throws Exception {
        var data = new UserUpdateDTO();
        data.setEmail("jack@yahoo.com");
        data.setPassword("new-password");

        mockMvc.perform(put("/api/users/" + testUser.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jack@yahoo.com"))
                // untouched fields stay the same
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        var user = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("jack@yahoo.com");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
    }

    @Test
    void testPartialUpdate() throws Exception {
        var data = Map.of("firstName", "Jane");

        mockMvc.perform(put("/api/users/" + testUser.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@google.com"));

        var user = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(passwordEncoder.matches("secret", user.getPassword())).isTrue();
    }

    @Test
    void testUpdateWithInvalidEmail() throws Exception {
        var data = Map.of("email", "not-an-email");

        mockMvc.perform(put("/api/users/" + testUser.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId()))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(testUser.getId())).isFalse();
    }
}
