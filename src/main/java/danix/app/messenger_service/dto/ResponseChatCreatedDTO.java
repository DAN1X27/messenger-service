package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChatCreatedDTO {
    private ResponseUserDTO user;
    private int createdChatId;
}
