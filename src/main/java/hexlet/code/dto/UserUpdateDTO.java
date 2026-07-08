package hexlet.code.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update: null means "field was not sent, keep the current value".
 * Constraints are ignored for null values, so only provided fields are validated.
 */
@Getter
@Setter
public class UserUpdateDTO {

    private String firstName;

    private String lastName;

    @Email
    private String email;

    @Size(min = 3)
    private String password;
}
