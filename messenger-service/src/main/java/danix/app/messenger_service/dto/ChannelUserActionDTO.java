package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelUserActionDTO {
    @NotEmpty(message = "Username must not be empty")
    private String username;
    @NotNull(message = "Channel id must not be empty")
    private Integer channelId;
}
