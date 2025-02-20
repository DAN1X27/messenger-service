package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseChannelUpdatingDTO {
    @JsonProperty("updated_channel")
    private ResponseChannelDTO updatedChannel;
    @JsonProperty("image_updated")
    private boolean isImageUpdated;
}
