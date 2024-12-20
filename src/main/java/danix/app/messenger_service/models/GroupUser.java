package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "group_users")
@Getter
@Setter
@NoArgsConstructor
public class GroupUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

    @Column(name = "is_admin")
    private boolean isAdmin;

    public String getUsername() {
        return user.getUsername();
    }

    public GroupUser(User user, Group group, boolean isAdmin) {
        this.user = user;
        this.group = group;
        this.isAdmin = isAdmin;
    }
}
