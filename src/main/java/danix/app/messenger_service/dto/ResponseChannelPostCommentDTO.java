package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseChannelPostCommentDTO {
    private String text;
    private Long id;
    @JsonProperty("content_type")
    private ContentType contentType;
    private ResponseUserDTO owner;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
