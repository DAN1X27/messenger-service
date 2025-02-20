package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChannelPostCommentDTO {
    @NotEmpty(message = "Comment cant be empty")
    @Size(max = 150, message = "Comment cant be more than 150 characters")
    private String comment;
    @NotNull(message = "Post id cant be empty")
    @JsonProperty("post_id")
    private Long postId;
}
