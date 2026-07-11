package hexlet.code.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update: null means "field was not sent, keep the current value".
 */
@Getter
@Setter
public class LabelUpdateDTO {

    @Size(min = 3, max = 1000)
    private String name;
}
