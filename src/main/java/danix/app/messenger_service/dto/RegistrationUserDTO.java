package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationUserDTO {

    @NotEmpty(message = "Email cannot be empty")
    @Email(message = "Email must be correct")
    private String email;

    @NotEmpty(message = "Username cannot be empty")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    private String username;

    @NotEmpty(message = "Password cannot be empty")
    private String password;

    @Size(max = 200, message = "Description must be up to 200 characters")
    private String description;

    @JsonProperty("private")
    private Boolean isPrivate;
}
