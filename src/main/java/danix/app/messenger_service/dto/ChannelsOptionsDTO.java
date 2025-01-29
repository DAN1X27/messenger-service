package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelsOptionsDTO {
    private Boolean isPrivate;
    private Boolean isPostsCommentsAllowed;
    private Boolean isImagesAllowed;
    private Boolean isInvitesAllowed;
}
