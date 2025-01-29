package danix.app.messenger_service.dto;

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
    private User.OnlineStatus onlineStatus;
    private boolean isBanned;
}
