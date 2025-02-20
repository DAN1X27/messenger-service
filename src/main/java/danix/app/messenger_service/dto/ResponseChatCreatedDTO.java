package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChatCreatedDTO {
    private ResponseUserDTO user;
    @JsonProperty("created_chat_id")
    private int createdChatId;
}
