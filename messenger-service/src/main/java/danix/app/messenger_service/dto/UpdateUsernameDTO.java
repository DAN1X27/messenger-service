package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUsernameDTO {

    @NotEmpty(message = "Username cannot be empty")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    private String username;

    @NotEmpty(message = "Password cannot be empty")
    private String password;
}
