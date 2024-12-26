package danix.app.messenger_service.dto;

import danix.app.messenger_service.models.ContentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChannelPostCommentDTO {
    private String text;
    private Long id;
    private ContentType contentType;
    private ResponseUserDTO owner;
}
