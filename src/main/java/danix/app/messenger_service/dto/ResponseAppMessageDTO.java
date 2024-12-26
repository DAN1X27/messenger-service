package danix.app.messenger_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAppMessageDTO {
    private String appMessageText;
    private LocalDateTime sentDate;
}
