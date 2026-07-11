package hexlet.code.controller;

import hexlet.code.component.DataInitializer;
import hexlet.code.dto.TaskStatusCreateDTO;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
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
class TaskStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private DataInitializer dataInitializer;

    private TaskStatus testStatus;

    @BeforeEach
    void setUp() {
        taskStatusRepository.deleteAll();

        testStatus = new TaskStatus();
        testStatus.setName("New");
        testStatus.setSlug("new");
        taskStatusRepository.save(testStatus);
    }

    @Test
    void testIndex() throws Exception {
        mockMvc.perform(get("/api/task_statuses").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("New"))
                .andExpect(jsonPath("$[0].slug").value("new"));
    }

    @Test
    void testShow() throws Exception {
        mockMvc.perform(get("/api/task_statuses/" + testStatus.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStatus.getId().intValue()))
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.slug").value("new"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testShowNotFound() throws Exception {
        mockMvc.perform(get("/api/task_statuses/99999").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreate() throws Exception {
        var data = new TaskStatusCreateDTO();
        data.setName("ToReview");
        data.setSlug("to_review");

        mockMvc.perform(post("/api/task_statuses")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("ToReview"))
                .andExpect(jsonPath("$.slug").value("to_review"))
                .andExpect(jsonPath("$.createdAt").exists());

        var status = taskStatusRepository.findBySlug("to_review").orElseThrow();
        assertThat(status.getName()).isEqualTo("ToReview");
        assertThat(status.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void testCreateWithEmptyName() throws Exception {
        var data = Map.of("name", "", "slug", "some_slug");

        mockMvc.perform(post("/api/task_statuses")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithEmptySlug() throws Exception {
        var data = Map.of("name", "SomeName", "slug", "");

        mockMvc.perform(post("/api/task_statuses")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithNotUniqueSlug() throws Exception {
        var data = Map.of("name", "AnotherName", "slug", "new");

        mockMvc.perform(post("/api/task_statuses")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdate() throws Exception {
        var data = Map.of("name", "newStatus");

        mockMvc.perform(put("/api/task_statuses/" + testStatus.getId())
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("newStatus"))
                // untouched fields stay the same
                .andExpect(jsonPath("$.slug").value("new"));

        var status = taskStatusRepository.findById(testStatus.getId()).orElseThrow();
        assertThat(status.getName()).isEqualTo("newStatus");
        assertThat(status.getSlug()).isEqualTo("new");
    }

    @Test
    void testUpdateWithEmptyName() throws Exception {
        var data = Map.of("name", "");

        mockMvc.perform(put("/api/task_statuses/" + testStatus.getId())
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/task_statuses/" + testStatus.getId()).with(jwt()))
                .andExpect(status().isNoContent());

        assertThat(taskStatusRepository.existsById(testStatus.getId())).isFalse();
    }

    @Test
    void testIndexWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/task_statuses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateWithoutAuth() throws Exception {
        var data = new TaskStatusCreateDTO();
        data.setName("ToReview");
        data.setSlug("to_review");

        mockMvc.perform(post("/api/task_statuses")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());

        assertThat(taskStatusRepository.findBySlug("to_review")).isEmpty();
    }

    @Test
    void testUpdateWithoutAuth() throws Exception {
        var data = Map.of("name", "newStatus");

        mockMvc.perform(put("/api/task_statuses/" + testStatus.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());

        var status = taskStatusRepository.findById(testStatus.getId()).orElseThrow();
        assertThat(status.getName()).isEqualTo("New");
    }

    @Test
    void testDeleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/task_statuses/" + testStatus.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(taskStatusRepository.existsById(testStatus.getId())).isTrue();
    }

    @Test
    void testFindBySlug() {
        var status = taskStatusRepository.findBySlug("new");
        assertThat(status).isPresent();
        assertThat(status.get().getName()).isEqualTo("New");

        assertThat(taskStatusRepository.findBySlug("unknown")).isEmpty();
    }

    @Test
    void testDefaultStatusesAreInitialized() {
        // the initializer already ran on application startup; re-run it
        // after setUp() cleaned the table to check the defaults it creates
        dataInitializer.run(null);

        for (var slug : new String[] {"draft", "to_review", "to_be_fixed", "to_publish", "published"}) {
            assertThat(taskStatusRepository.findBySlug(slug)).isPresent();
        }
    }
}
