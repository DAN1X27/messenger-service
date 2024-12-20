package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateChannelPostCommentDTO {
    @NotNull(message = "Comment id cant be empty")
    private Long commentId;
    @NotEmpty(message = "Comment cant be empty")
    @Size(max = 150, message = "Comment cant be more than 150 characters")
    private String comment;
}
