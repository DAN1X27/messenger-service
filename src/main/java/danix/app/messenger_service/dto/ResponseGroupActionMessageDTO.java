package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseGroupActionMessageDTO {
    @JsonProperty("action_message")
    private String text;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
}
