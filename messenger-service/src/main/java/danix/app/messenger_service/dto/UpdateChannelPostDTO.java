package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class UpdateChannelPostDTO {
    @NotNull(message = "Post id must not be empty")
    private Long id;
    @NotEmpty(message = "Text of the post must not be empty")
    @Size(min = 1, max = 300, message = "Text of the post can't be more than 300 characters")
    private String text;
}
