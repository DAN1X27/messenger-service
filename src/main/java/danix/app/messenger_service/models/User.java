package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Person")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    private String description;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status")
    private OnlineStatus onlineStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Roles role;

    @OneToMany(mappedBy = "user")
    private List<BannedUser> bannedUser;

    @OneToMany(mappedBy = "owner")
    private List<UserFriend> userFriends;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status userStatus;

    @OneToMany(mappedBy = "user1")
    private List<Chat> chats;

    @OneToMany(mappedBy = "messageOwner")
    private List<GroupMessage> groupMessages;

    @OneToMany(mappedBy = "owner")
    private List<Group> groupsOwner;

    @ManyToMany(mappedBy = "bannedUsers")
    private List<Group> bannedGroups;

    @OneToMany(mappedBy = "user")
    private List<GroupUser> groups;

    @OneToMany(mappedBy = "owner")
    private List<Channel> ownedChannels;

    @OneToMany(mappedBy = "user")
    private List<ChannelUser> channels;

    @OneToMany(mappedBy = "user")
    private List<AppMessage> appMessages;

    @ManyToMany(mappedBy = "bannedUsers")
    private List<Channel> bannedChannels;

    @OneToMany(mappedBy = "user")
    private List<GroupInvite> groupInvites;

    @OneToMany(mappedBy = "user")
    private List<ChannelInvite> channelInvites;

    @Column(name = "image")
    private String imageUUID;

    @Column(name = "web_socket_uuid")
    private String webSocketUUID;

    @ManyToMany(mappedBy = "likes")
    private List<ChannelPost> postsLikes;

    @Column(name = "is_banned")
    private boolean isBanned;

    @Column(name = "last_online_status_update")
    private LocalDateTime lastOnlineStatusUpdate;

    public enum Status {
        REGISTERED,
        BANNED,
        TEMPORALLY_REGISTERED
    }

    public enum Roles {
        ROLE_USER,
        ROLE_ADMIN
    }

    public enum OnlineStatus {
        ONLINE,
        OFFLINE
    }
}