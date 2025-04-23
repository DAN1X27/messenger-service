package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "channels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private String description;

    private String image;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private User owner;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "is_private")
    private boolean isPrivate;

    @Column(name = "web_socket_uuid")
    private String webSocketUUID;

    @OneToMany(mappedBy = "channel")
    private List<ChannelUser> users;

    @OneToMany(mappedBy = "channel")
    private List<ChannelPost> posts;

    @Column(name = "is_banned")
    private boolean isBaned;

    @Column(name = "is_posts_comments_allowed")
    private boolean isPostsCommentsAllowed;

    @Column(name = "is_files_allowed")
    private boolean isFilesAllowed;

    @Column(name = "is_invites_allowed")
    private boolean isInvitesAllowed;

    @ManyToMany
    @JoinTable(name = "banned_channels_users",
    joinColumns = @JoinColumn(name = "channel_id"),
    inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> bannedUsers;

    @OneToMany(mappedBy = "channel")
    private List<ChannelLog> logs;

    @OneToMany(mappedBy = "channel")
    private List<ChannelInvite> invites;
}
