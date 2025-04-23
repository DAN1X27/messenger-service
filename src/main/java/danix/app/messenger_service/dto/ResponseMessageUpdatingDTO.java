package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResponseMessageUpdatingDTO {
    @JsonProperty("message_id")
    private Long id;
    @JsonProperty("updated_message")
    private String text;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
    @JsonProperty("sender_id")
    private int senderId;
}
