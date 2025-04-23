package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.ContentType;
import lombok.*;

import java.time.LocalDateTime;

@Data
public class ResponseGroupMessageDTO {
    private long id;
    private String text;
    private ResponseUserDTO sender;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
    @JsonProperty("content_type")
    private ContentType contentType;
}
