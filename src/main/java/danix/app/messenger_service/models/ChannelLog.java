package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "channels_logs")
@Getter
@Setter
public class ChannelLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String message;
    @ManyToOne
    @JoinColumn(name = "channel_id", referencedColumnName = "id")
    private Channel channel;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "expired_time")
    private LocalDateTime expiredTime;

    public ChannelLog() {
        createdAt = LocalDateTime.now();
        expiredTime = LocalDateTime.now().plusDays(14);
    }
}
