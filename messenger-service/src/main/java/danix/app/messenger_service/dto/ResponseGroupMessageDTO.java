package danix.app.messenger_service.dto;

import danix.app.messenger_service.models.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseGroupMessageDTO {
    private String message;
    private ResponseUserDTO sender;
    private LocalDateTime sentTime;
    private long messageId;
    private ContentType contentType;
}
