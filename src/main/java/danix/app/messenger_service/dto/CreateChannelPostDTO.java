package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChannelPostDTO {
    @NotEmpty(message = "Text of the post must not be empty")
    @Size(min = 1, max = 300, message = "Text of the post can't be more than 300 characters")
    private String text;
    @NotNull(message = "Channel id can't be empty")
    @JsonProperty("channel_id")
    private Integer channelId;
}
