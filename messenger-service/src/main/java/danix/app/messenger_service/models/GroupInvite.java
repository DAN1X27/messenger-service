package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "groups_invites")
@Getter
@Setter
public class GroupInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "expired_time")
    private LocalDateTime expiredTime;

    public GroupInvite() {
        sentTime = LocalDateTime.now();
        expiredTime = LocalDateTime.now().plusDays(3);
    }
}
