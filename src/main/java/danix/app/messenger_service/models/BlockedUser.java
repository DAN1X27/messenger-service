package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Blocked_Users")
@Getter
@Setter
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "blocked_user", referencedColumnName = "id")
    private User blockedUser;

    public BlockedUser(User owner, User blockedUser) {
        this.owner = owner;
        this.blockedUser = blockedUser;
    }

    public BlockedUser() {}
}
