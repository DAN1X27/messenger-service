package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "groups_actions_messages")
@Getter
@Setter
public class GroupActionMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String message;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    public GroupActionMessage() {
        this.sentTime = LocalDateTime.now();
    }
}
