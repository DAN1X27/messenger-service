package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "groups")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private User owner;

    @Column(name = "created_at")
    private Date createdAt;

    @OneToMany(mappedBy = "group")
    private List<GroupUser> users;

    @OneToMany(mappedBy = "group")
    private List<GroupMessage> messages;

    @ManyToMany
    @JoinTable(name = "groups_banned_users",
    joinColumns = @JoinColumn(name = "group_id"),
    inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> bannedUsers;

    @OneToMany(mappedBy = "group")
    private List<GroupActionMessage> actionMessages;

    @Column(name = "image")
    private String image;

    @Column(name = "web_socket_uuid")
    private String webSocketUUID;

    private String description;
}
