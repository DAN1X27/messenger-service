package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseGroupUpdatingDTO {
    @JsonProperty("updated_group")
    private ResponseGroupDTO updatedGroup;
    @JsonProperty("image_updated")
    private boolean isImageUpdated;
}
