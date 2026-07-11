package hexlet.code.controller;

import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private TaskStatus draftStatus;
    private TaskStatus reviewStatus;
    private Task testTask;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        taskStatusRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("john@google.com");
        testUser.setPassword(passwordEncoder.encode("secret"));
        userRepository.save(testUser);

        draftStatus = new TaskStatus();
        draftStatus.setName("Draft");
        draftStatus.setSlug("draft");
        taskStatusRepository.save(draftStatus);

        reviewStatus = new TaskStatus();
        reviewStatus.setName("ToReview");
        reviewStatus.setSlug("to_review");
        taskStatusRepository.save(reviewStatus);

        testTask = new Task();
        testTask.setName("Task 1");
        testTask.setIndex(3140);
        testTask.setDescription("Description of task 1");
        testTask.setTaskStatus(draftStatus);
        testTask.setAssignee(testUser);
        taskRepository.save(testTask);
    }

    @Test
    void testIndex() throws Exception {
        mockMvc.perform(get("/api/tasks").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Task 1"))
                .andExpect(jsonPath("$[0].status").value("draft"));
    }

    @Test
    void testShow() throws Exception {
        mockMvc.perform(get("/api/tasks/" + testTask.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testTask.getId().intValue()))
                .andExpect(jsonPath("$.index").value(3140))
                .andExpect(jsonPath("$.title").value("Task 1"))
                .andExpect(jsonPath("$.content").value("Description of task 1"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.assignee_id").value(testUser.getId().intValue()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testShowNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/99999").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreate() throws Exception {
        var data = Map.of(
                "index", 12,
                "assignee_id", testUser.getId(),
                "title", "Test title",
                "content", "Test content",
                "status", "draft"
        );

        mockMvc.perform(post("/api/tasks")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.index").value(12))
                .andExpect(jsonPath("$.title").value("Test title"))
                .andExpect(jsonPath("$.content").value("Test content"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.assignee_id").value(testUser.getId().intValue()))
                .andExpect(jsonPath("$.createdAt").exists());

        var task = taskRepository.findAll().stream()
                .filter(t -> t.getName().equals("Test title"))
                .findFirst()
                .orElseThrow();
        assertThat(task.getDescription()).isEqualTo("Test content");
        assertThat(task.getTaskStatus().getSlug()).isEqualTo("draft");
        assertThat(task.getAssignee().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void testCreateWithoutAssignee() throws Exception {
        var data = Map.of("title", "Unassigned task", "status", "draft");

        mockMvc.perform(post("/api/tasks")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Unassigned task"))
                .andExpect(jsonPath("$.assignee_id").doesNotExist());
    }

    @Test
    void testCreateWithoutTitle() throws Exception {
        var data = Map.of("status", "draft");

        mockMvc.perform(post("/api/tasks")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithoutStatus() throws Exception {
        var data = Map.of("title", "Test title");

        mockMvc.perform(post("/api/tasks")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithUnknownStatus() throws Exception {
        var data = Map.of("title", "Test title", "status", "no_such_status");

        mockMvc.perform(post("/api/tasks")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdate() throws Exception {
        var data = Map.of("title", "New title", "content", "New content");

        mockMvc.perform(put("/api/tasks/" + testTask.getId())
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.content").value("New content"))
                // untouched fields stay the same
                .andExpect(jsonPath("$.index").value(3140))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.assignee_id").value(testUser.getId().intValue()));

        var task = taskRepository.findById(testTask.getId()).orElseThrow();
        assertThat(task.getName()).isEqualTo("New title");
        assertThat(task.getDescription()).isEqualTo("New content");
        assertThat(task.getTaskStatus().getSlug()).isEqualTo("draft");
        assertThat(task.getAssignee().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void testUpdateStatus() throws Exception {
        var data = Map.of("status", "to_review");

        mockMvc.perform(put("/api/tasks/" + testTask.getId())
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("to_review"))
                .andExpect(jsonPath("$.title").value("Task 1"));

        var task = taskRepository.findById(testTask.getId()).orElseThrow();
        assertThat(task.getTaskStatus().getSlug()).isEqualTo("to_review");
    }

    @Test
    void testUpdateWithEmptyTitle() throws Exception {
        var data = Map.of("title", "");

        mockMvc.perform(put("/api/tasks/" + testTask.getId())
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/tasks/" + testTask.getId()).with(jwt()))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.existsById(testTask.getId())).isFalse();
    }

    @Test
    void testIndexWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testShowWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/tasks/" + testTask.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateWithoutAuth() throws Exception {
        var data = Map.of("title", "Test title", "status", "draft");

        mockMvc.perform(post("/api/tasks")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateWithoutAuth() throws Exception {
        var data = Map.of("title", "New title");

        mockMvc.perform(put("/api/tasks/" + testTask.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());

        var task = taskRepository.findById(testTask.getId()).orElseThrow();
        assertThat(task.getName()).isEqualTo("Task 1");
    }

    @Test
    void testDeleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/tasks/" + testTask.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(taskRepository.existsById(testTask.getId())).isTrue();
    }

    @Test
    void testDeleteUserWithTaskIsForbidden() throws Exception {
        // authenticate as the user themselves: deleting a user requires it
        mockMvc.perform(delete("/api/users/" + testUser.getId())
                        .with(jwt().jwt(token -> token.subject(testUser.getEmail()))))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.existsById(testUser.getId())).isTrue();
    }

    @Test
    void testDeleteStatusWithTaskIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/task_statuses/" + draftStatus.getId()).with(jwt()))
                .andExpect(status().isBadRequest());

        assertThat(taskStatusRepository.existsById(draftStatus.getId())).isTrue();
    }

    @Test
    void testUnusedStatusCanBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/task_statuses/" + reviewStatus.getId()).with(jwt()))
                .andExpect(status().isNoContent());

        assertThat(taskStatusRepository.existsById(reviewStatus.getId())).isFalse();
    }
}
