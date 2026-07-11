package hexlet.code.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for filtering tasks: null means "do not filter by this field".
 */
@Getter
@Setter
public class TaskParamsDTO {

    private String titleCont;

    private Long assigneeId;

    private String status;

    private Long labelId;
}
