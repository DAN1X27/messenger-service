package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseGroupUserDTO {
    private String username;
    private int id;
    @JsonProperty("admin")
    private boolean isAdmin;
    @JsonProperty("online_status")
    private User.OnlineStatus onlineStatus;
}
