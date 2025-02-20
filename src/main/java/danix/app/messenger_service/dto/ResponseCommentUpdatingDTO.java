package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseCommentUpdatingDTO {
    @JsonProperty("updated_comment_id")
    private Long updatedCommentId;
    @JsonProperty("updated_comment_text")
    private String updatedCommentText;
}
