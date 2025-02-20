package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "Users_Chats")
@Getter
@Setter
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "user1", referencedColumnName = "id")
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2", referencedColumnName = "id")
    private User user2;

    @Column(name = "web_socket_uuid")
    private String webSocketUUID;

    @OneToMany(mappedBy = "chat")
    private List<ChatMessage> messages;

    public Chat(User user1, User user2, String webSocketUUID) {
        this.user1 = user1;
        this.user2 = user2;
        this.webSocketUUID = webSocketUUID;
    }

    public Chat() {}
}
