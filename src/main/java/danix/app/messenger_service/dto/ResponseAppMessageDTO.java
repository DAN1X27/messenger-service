package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAppMessageDTO {
    @JsonProperty("app_message")
    private String appMessageText;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
}
