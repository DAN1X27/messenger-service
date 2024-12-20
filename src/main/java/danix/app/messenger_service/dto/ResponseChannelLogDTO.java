package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseChannelLogDTO {
    private String message;
    private LocalDateTime createdAt;
}
