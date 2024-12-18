package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGroupDTO {
    private String name;
    private String description;
    @NotNull(message = "Group id cannot be empty")
    private Integer groupId;
}
