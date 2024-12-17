package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "groups_messages")
@Getter
@Setter
@NoArgsConstructor
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    private String message;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "message_owner", referencedColumnName = "id")
    private User messageOwner;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentType contentType;
}
