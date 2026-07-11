package hexlet.code.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Partial update: null means "field was not sent, keep the current value".
 * Constraints are ignored for null values, so only provided fields are validated.
 */
@Getter
@Setter
public class TaskUpdateDTO {

    @Size(min = 1)
    private String title;

    private Integer index;

    @JsonProperty("assignee_id")
    private Long assigneeId;

    private String content;

    @Size(min = 1)
    private String status;

    private Set<Long> taskLabelIds;
}
