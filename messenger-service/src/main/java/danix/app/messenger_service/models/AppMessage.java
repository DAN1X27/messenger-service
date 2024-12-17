package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_messages")
@Getter
@Setter
@NoArgsConstructor
public class AppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "message")
    private String message;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "remove_date")
    private LocalDateTime removeDate;

    public AppMessage(String message, User user) {
        this.message = message;
        this.user = user;
        sentDate = LocalDateTime.now();
        removeDate = LocalDateTime.now().plusDays(3);
    }
}
