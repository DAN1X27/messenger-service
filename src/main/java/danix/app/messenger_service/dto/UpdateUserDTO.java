package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO {
    @NotEmpty(message = "Username must not be empty")
    private String username;
    @Size(max = 200, message = "Description length should not be more than 200 characters")
    private String description;
    @JsonProperty("is_private")
    private boolean isPrivate;
}
