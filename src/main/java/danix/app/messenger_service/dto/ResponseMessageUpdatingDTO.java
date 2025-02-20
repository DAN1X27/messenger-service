package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseMessageUpdatingDTO {
    @JsonProperty("message_id")
    private Long updatedMessageId;
    @JsonProperty("updated_message")
    private String updatedMessage;
}
