package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateChannelDTO {
    @Size(max = 30, message = "Channel name must be between 5 and 30 characters")
    private String name;
    @Size(max = 200, message = "Channel description can't be more than 200 characters")
    private String description;
}
