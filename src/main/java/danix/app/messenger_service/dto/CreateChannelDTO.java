package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChannelDTO {

    @NotEmpty(message = "Channel name must not be empty")
    @Size(min = 3, max = 30, message = "Channel name must be between 5 and 30 characters")
    private String name;

    @Size(max = 200, message = "Channel description can't be more than 200 characters")
    private String description;

    @JsonProperty("private")
    private Boolean isPrivate;
}
