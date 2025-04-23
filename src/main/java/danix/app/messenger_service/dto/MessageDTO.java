package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageDTO {
    @NotBlank(message = "Message must not be empty")
    private String message;
}
