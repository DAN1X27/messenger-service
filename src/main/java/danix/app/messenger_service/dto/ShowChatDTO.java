package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowChatDTO {
    private int id;
    private List<ResponseChatMessageDTO> messages;
    private ResponseUserDTO user;
    @JsonProperty("web_socket")
    private String webSocketUUID;
}
