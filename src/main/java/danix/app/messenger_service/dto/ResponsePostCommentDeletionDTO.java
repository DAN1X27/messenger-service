package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Deprecated
public class ResponsePostCommentDeletionDTO {
    @JsonProperty("deleted_comment_id")
    private Long deletedCommentId;
}
