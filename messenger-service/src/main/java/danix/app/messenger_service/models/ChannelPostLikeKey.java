package danix.app.messenger_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Embeddable
@Data
public class ChannelPostLikeKey {

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "user_id")
    private Integer userId;
}
