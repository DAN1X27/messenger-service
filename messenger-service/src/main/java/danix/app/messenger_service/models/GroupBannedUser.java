package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "groups_banned_users")
@Getter
@Setter
public class GroupBannedUser {
    @EmbeddedId
    private BannedGroupUserKey id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;
}
