package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShowChatDTO {
    private int id;
    private List<ResponseChatMessageDTO> messages;
    private String user1Username;
    private String user2Username;
}
