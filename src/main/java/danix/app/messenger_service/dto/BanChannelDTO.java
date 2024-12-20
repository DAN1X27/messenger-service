package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BanChannelDTO {
    @NotEmpty(message = "Reason cant be empty")
    private String reason;
    @NotNull(message = "Channel id cant be empty")
    private Integer channelId;
}
