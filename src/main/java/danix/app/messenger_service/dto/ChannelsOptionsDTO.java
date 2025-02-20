package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelsOptionsDTO {
    @JsonProperty("private")
    private Boolean isPrivate;
    @JsonProperty("posts_comments_allowed")
    private Boolean isPostsCommentsAllowed;
    @JsonProperty("files_allowed")
    private Boolean isFilesAllowed;
    @JsonProperty("invites_allowed")
    private Boolean isInvitesAllowed;
}
