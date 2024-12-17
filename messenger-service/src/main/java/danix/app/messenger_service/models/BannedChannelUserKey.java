package danix.app.messenger_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class BannedChannelUserKey implements Serializable {

    @Column(name = "channel_id")
    private Integer channelId;

    @Column(name = "user_id")
    private Integer userId;
}
