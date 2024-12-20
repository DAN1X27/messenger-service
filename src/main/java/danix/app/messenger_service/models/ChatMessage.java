package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Chats_Messages")
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne
    @JoinColumn(name = "chat", referencedColumnName = "id")
    private Chat chat;

    @Column(name = "message")
    private String message;

    @ManyToOne
    @JoinColumn(name = "message_owner", referencedColumnName = "id")
    private User owner;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "is_read")
    private boolean isRead;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentType contentType;

    public ChatMessage() {
        sentTime = LocalDateTime.now();
        isRead = false;
    }
}
