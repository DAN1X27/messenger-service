package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "channels")
@Data
@NoArgsConstructor
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

    public static Builder builder() {
        return new Builder();
    }

    public Channel(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.isPrivate = builder.isPrivate;
        this.createdAt = builder.createdAt;
        this.owner = builder.owner;
        this.image = builder.image;
        this.isPostsCommentsAllowed = true;
        this.isFilesAllowed = true;
        this.isInvitesAllowed = true;
        this.webSocketUUID = builder.webSocketUUID;
    }

    public static class Builder {
        private User owner;
        private Date createdAt;
        private boolean isPrivate;
        private String name;
        private String description;
        private String image;
        private String webSocketUUID;

        public Builder webSocketUUID(String webSocketUUID) {
            this.webSocketUUID = webSocketUUID;
            return this;
        }

        public Builder owner(User owner) {
            this.owner = owner;
            return this;
        }

        public Builder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder isPrivate(boolean isPrivate) {
            this.isPrivate = isPrivate;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Channel build() {
            return new Channel(this);
        }
    }
}
