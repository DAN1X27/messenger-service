package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponsePostUpdatingDTO {
    @JsonProperty("updated_post")
    private ResponseChannelPostDTO updatedPost;
}
