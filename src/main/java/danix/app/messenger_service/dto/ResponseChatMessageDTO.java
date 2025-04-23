package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.ContentType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseChatMessageDTO {
    @JsonProperty("message_id")
    private long id;
    @JsonProperty("message")
    private String text;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
    @JsonProperty("read")
    private boolean isRead;
    @JsonProperty("type")
    private ContentType contentType;
    private ResponseUserDTO sender;
}
