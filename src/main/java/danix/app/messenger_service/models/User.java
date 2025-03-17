package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Person")
@Data
@NoArgsConstructor
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

    public static Builder builder() {
        return new Builder();
    }

    private User(Builder builder) {
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.description = builder.description;
        this.isPrivate = builder.isPrivate;
        this.createdAt = builder.createdAt;
        this.role = builder.role;
        this.userStatus = builder.userStatus;
        this.imageUUID = builder.imageUUID;
        this.onlineStatus = OnlineStatus.ONLINE;
        this.webSocketUUID = builder.webSocketUUID;
        this.isBanned = false;
        this.lastOnlineStatusUpdate = LocalDateTime.now();
    }

    public static class Builder {
        private String username;
        private String email;
        private String password;
        private String description;
        private Boolean isPrivate;
        private LocalDateTime createdAt;
        private Roles role;
        private Status userStatus;
        private String imageUUID;
        private String webSocketUUID;

        public Builder webSocketUUID(String webSocketUUID) {
            this.webSocketUUID = webSocketUUID;
            return this;
        }

        public Builder imageUUID(String imageUUID) {
            this.imageUUID = imageUUID;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder isPrivate(Boolean isPrivate) {
            this.isPrivate = isPrivate;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder role(Roles role) {
            this.role = role;
            return this;
        }

        public Builder userStatus(Status userStatus) {
            this.userStatus = userStatus;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

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