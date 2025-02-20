package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowUserDTO {
    private int id;
    private String username;
    private String description;
    @JsonProperty("online_status")
    private User.OnlineStatus onlineStatus;
    @JsonProperty("banned")
    private boolean isBanned;
    @JsonProperty("private")
    private boolean isPrivate;
}
