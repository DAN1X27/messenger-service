package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ResponseGroupActionMessageDTO {
    private String actionMessageText;
    private LocalDateTime sentTime;

    public ResponseGroupActionMessageDTO(String actionMessageText, LocalDateTime sentTime) {
        this.actionMessageText = actionMessageText;
        this.sentTime = sentTime;
    }
}
