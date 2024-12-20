package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordDTO {

    @NotEmpty(message = "Old password cannot be empty")
    private String password;

    @NotEmpty(message = "New password cannot be empty")
    @Size(min = 5, max = 30, message = "New password must be between 5 and 30 characters")
    private String newPassword;
}
