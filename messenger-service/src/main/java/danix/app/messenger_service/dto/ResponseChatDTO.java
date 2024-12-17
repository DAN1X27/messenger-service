package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChatDTO {
    private ResponseUserDTO user;
    private int id;
}
