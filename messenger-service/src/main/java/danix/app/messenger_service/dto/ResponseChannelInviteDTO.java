package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseChannelInviteDTO {
    private int channelId;
    private String channelName;
    private LocalDateTime sendTime;
}
