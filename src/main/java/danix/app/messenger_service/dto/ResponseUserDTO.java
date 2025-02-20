package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseUserDTO {
    private Integer id;
    private String username;
    @JsonProperty("online_status")
    private User.OnlineStatus onlineStatus;
    @JsonProperty("banned")
    private boolean isBanned;
}
