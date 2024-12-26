package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "channels_posts_likes")
@Data
public class ChannelPostLike {

    @EmbeddedId
    private ChannelPostLikeKey id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private ChannelPost post;
}
