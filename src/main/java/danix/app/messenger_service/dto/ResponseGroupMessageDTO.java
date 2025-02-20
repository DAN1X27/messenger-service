package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseGroupMessageDTO {
    private String message;
    private ResponseUserDTO sender;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
    @JsonProperty("message_id")
    private long messageId;
    @JsonProperty("content_type")
    private ContentType contentType;
}
