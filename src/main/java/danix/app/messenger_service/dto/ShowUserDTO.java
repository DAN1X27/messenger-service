package danix.app.messenger_service.dto;

import danix.app.messenger_service.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowUserDTO {
    private int id;
    private String username;
    private String description;
    private User.OnlineStatus onlineStatus;
    private boolean isBanned;
    private boolean isPrivate;
}
