package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Users_Friends")
@Getter
@Setter
public class UserFriend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "friend_id", referencedColumnName = "id")
    private User friend;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FriendsStatus status;

    public UserFriend(User owner, User friend, FriendsStatus status) {
        this.owner = owner;
        this.friend = friend;
        this.status = status;
    }

    public UserFriend() {}

    public enum FriendsStatus {
        WAITING,
        ACCEPTED
    }
}
