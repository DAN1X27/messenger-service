package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ResponseGroupActionMessageDTO {
    @JsonProperty("notification")
    private String actionMessageText;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;

    public ResponseGroupActionMessageDTO(String actionMessageText, LocalDateTime sentTime) {
        this.actionMessageText = actionMessageText;
        this.sentTime = sentTime;
    }
}
