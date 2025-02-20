package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseUpdateUserOnlineStatusDTO {
    @JsonProperty("updated_user_online_status_id")
    private Integer updatedUserOnlineStatusId;
    @JsonProperty("online_status")
    private User.OnlineStatus onlineStatus;
}
