package danix.app.messenger_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseGroupUpdatingDTO {
    private ResponseGroupDTO updatedGroup;
    private boolean isImageUpdated;
}
