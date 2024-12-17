package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channels_users")
@Data
@NoArgsConstructor
public class ChannelUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "channel_id", referencedColumnName = "id")
    private Channel channel;

    @Column(name = "is_admin")
    private Boolean isAdmin;

    public String getUsername() {
        return user.getUsername();
    }
}
