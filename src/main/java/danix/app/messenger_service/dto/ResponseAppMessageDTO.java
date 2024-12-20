package danix.app.messenger_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ResponseAppMessageDTO {
    private String message;
    private LocalDateTime sentDate;
}
