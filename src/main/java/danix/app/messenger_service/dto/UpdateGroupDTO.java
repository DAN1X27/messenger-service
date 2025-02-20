package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGroupDTO {
    private String name;
    private String description;
    @NotNull(message = "Group id cannot be empty")
    @JsonProperty("group_id")
    private Integer groupId;
}
