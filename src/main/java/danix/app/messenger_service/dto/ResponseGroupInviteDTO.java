package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseGroupInviteDTO {
    private String groupName;
    private int groupId;
    private LocalDateTime invitedAt;
}
