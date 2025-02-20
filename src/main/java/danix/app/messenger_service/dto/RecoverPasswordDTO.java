package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecoverPasswordDTO implements RequestEmailKey {

    @NotEmpty(message = "Email cannot be empty!")
    private String email;

    @NotEmpty(message = "Password cannot be empty!")
    @Size(min = 5, max = 30, message = "Password must be between 5 and 30 characters")
    @JsonProperty("new_password")
    private String newPassword;

    @NotNull(message = "Key cannot be empty!")
    private Integer key;
}
