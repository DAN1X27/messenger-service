package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.ContentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseChannelPostDTO {
    private long id;
    private String text;
    @JsonProperty("comments_count")
    private int commentsCount;
    private int likes;
    @JsonProperty("liked")
    private boolean isLiked;
    @JsonProperty("content_type")
    private ContentType contentType;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    private ResponseUserDTO owner;
    private List<ResponseChannelPostFilesDTO> files;
}
