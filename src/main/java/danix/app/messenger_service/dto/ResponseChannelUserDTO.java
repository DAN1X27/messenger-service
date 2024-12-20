package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChannelUserDTO {
    private int id;
    private String username;
    private Boolean isAdmin;
}
