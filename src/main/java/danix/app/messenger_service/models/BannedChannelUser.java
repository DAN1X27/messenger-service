package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "banned_channels_users")
@Getter
@Setter
public class BannedChannelUser {

    @EmbeddedId
    private BannedChannelUserKey id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("channelId")
    @JoinColumn(name = "channel_id")
    private Channel channel;
}
