package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseGroupInviteDTO {
    @JsonProperty("group_name")
    private String groupName;
    @JsonProperty("group_id")
    private int groupId;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
}
