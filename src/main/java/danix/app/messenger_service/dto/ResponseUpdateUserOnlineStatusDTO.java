package danix.app.messenger_service.dto;

import danix.app.messenger_service.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseUpdateUserOnlineStatusDTO {
    private Integer updatedUserOnlineStatusId;
    private User.OnlineStatus onlineStatus;
}
