package hexlet.code.controller;

import hexlet.code.model.User;
import hexlet.code.repository.TaskRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // tasks may reference users via a foreign key, clean them up first
        taskRepository.deleteAll();
        userRepository.deleteAll();

        var user = new User();
        user.setEmail("ivan@google.com");
        user.setPassword(passwordEncoder.encode("some-password"));
        userRepository.save(user);
    }

    @Test
    void testLogin() throws Exception {
        var data = Map.of("username", "ivan@google.com", "password", "some-password");

        var result = mockMvc.perform(post("/api/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andReturn();

        var token = result.getResponse().getContentAsString();
        assertThat(token).isNotBlank();
        // JWT: three dot-separated parts
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void testLoginWithWrongPassword() throws Exception {
        var data = Map.of("username", "ivan@google.com", "password", "wrong-password");

        mockMvc.perform(post("/api/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLoginWithUnknownUser() throws Exception {
        var data = Map.of("username", "nobody@google.com", "password", "some-password");

        mockMvc.perform(post("/api/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());
    }
}
