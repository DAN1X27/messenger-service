package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupDTO {
    @NotEmpty(message = "Group name must not be empty")
    @Size(min = 5, max = 50, message = "Group name must be between 5 and 50 characters")
    private String name;
    @Size(max = 200, message = "Description must be up to 200 characters")
    private String description;
}
