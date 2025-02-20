package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseChannelInviteDTO {
    @JsonProperty("channel_id")
    private int channelId;
    @JsonProperty("channel_name")
    private String channelName;
    @JsonProperty("sent_time")
    private LocalDateTime sendTime;
}
