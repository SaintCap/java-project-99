package hexlet.code.controller;

import hexlet.code.component.DataInitializer;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataInitializer dataInitializer;

    private Label testLabel;

    @BeforeEach
    void setUp() {
        // tasks reference labels, statuses and users via foreign keys, clean them up first
        taskRepository.deleteAll();
        labelRepository.deleteAll();
        taskStatusRepository.deleteAll();
        userRepository.deleteAll();

        testLabel = new Label();
        testLabel.setName("urgent");
        labelRepository.save(testLabel);
    }

    @Test
    void testIndex() throws Exception {
        mockMvc.perform(get("/api/labels").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("urgent"));
    }

    @Test
    void testShow() throws Exception {
        mockMvc.perform(get("/api/labels/" + testLabel.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLabel.getId().intValue()))
                .andExpect(jsonPath("$.name").value("urgent"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testShowNotFound() throws Exception {
        mockMvc.perform(get("/api/labels/99999").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreate() throws Exception {
        var data = Map.of("name", "new label");

        mockMvc.perform(post("/api/labels")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("new label"))
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(labelRepository.findByName("new label")).isPresent();
    }

    @Test
    void testCreateWithTooShortName() throws Exception {
        var data = Map.of("name", "ab");

        mockMvc.perform(post("/api/labels")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithNotUniqueName() throws Exception {
        var data = Map.of("name", "urgent");

        mockMvc.perform(post("/api/labels")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdate() throws Exception {
        var data = Map.of("name", "not urgent");

        mockMvc.perform(put("/api/labels/" + testLabel.getId())
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("not urgent"));

        var label = labelRepository.findById(testLabel.getId()).orElseThrow();
        assertThat(label.getName()).isEqualTo("not urgent");
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/labels/" + testLabel.getId()).with(jwt()))
                .andExpect(status().isNoContent());

        assertThat(labelRepository.existsById(testLabel.getId())).isFalse();
    }

    @Test
    void testDeleteLabelWithTaskIsForbidden() throws Exception {
        var status = new TaskStatus();
        status.setName("Draft");
        status.setSlug("draft");
        taskStatusRepository.save(status);

        var task = new Task();
        task.setName("Task with label");
        task.setTaskStatus(status);
        task.setLabels(new HashSet<>(Set.of(testLabel)));
        taskRepository.save(task);

        mockMvc.perform(delete("/api/labels/" + testLabel.getId()).with(jwt()))
                .andExpect(status().isBadRequest());

        assertThat(labelRepository.existsById(testLabel.getId())).isTrue();
    }

    @Test
    void testIndexWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/labels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateWithoutAuth() throws Exception {
        var data = Map.of("name", "new label");

        mockMvc.perform(post("/api/labels")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateWithoutAuth() throws Exception {
        var data = Map.of("name", "not urgent");

        mockMvc.perform(put("/api/labels/" + testLabel.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/labels/" + testLabel.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(labelRepository.existsById(testLabel.getId())).isTrue();
    }

    @Test
    void testFindByName() {
        var label = labelRepository.findByName("urgent");
        assertThat(label).isPresent();
        assertThat(label.get().getId()).isEqualTo(testLabel.getId());

        assertThat(labelRepository.findByName("unknown")).isEmpty();
    }

    @Test
    void testDefaultLabelsAreInitialized() {
        // the initializer already ran on application startup; re-run it
        // after setUp() cleaned the table to check the defaults it creates
        dataInitializer.run(null);

        for (var name : new String[] {"feature", "bug"}) {
            assertThat(labelRepository.findByName(name)).isPresent();
        }
    }
}
