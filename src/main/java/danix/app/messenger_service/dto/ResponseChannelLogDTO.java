package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseChannelLogDTO {
    private String message;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
