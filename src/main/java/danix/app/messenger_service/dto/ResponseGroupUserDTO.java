package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseGroupUserDTO {
    private String username;
    private int id;
    private boolean isAdmin;
}
