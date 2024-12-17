package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Banned_Users")
@Getter
@Setter
public class BannedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "reason")
    private String reason;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    public BannedUser(String reason, User user) {
        this.reason = reason;
        this.user = user;
    }

    public BannedUser() {}
}
