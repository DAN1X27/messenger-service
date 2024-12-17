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
    private Boolean isPrivate;

    @OneToMany(mappedBy = "channel")
    private List<ChannelUser> users;

    @OneToMany(mappedBy = "channel")
    private List<ChannelPost> posts;

    @Column(name = "is_banned")
    private boolean isBaned;

    @OneToMany(mappedBy = "channel")
    private List<BannedChannelUser> bannedUsers;

    @OneToMany(mappedBy = "channel")
    private List<ChannelLog> logs;

    public static Builder builder() {
        return new Builder();
    }

    public Channel(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.isPrivate = builder.isPrivate;
        this.createdAt = builder.createdAt;
        this.isBaned = builder.isBanned;
        this.owner = builder.owner;
        this.image = builder.image;
    }

    public static class Builder {
        private User owner;
        private Date createdAt;
        private Boolean isPrivate;
        private boolean isBanned;
        private String name;
        private String description;
        private String image;

        public Builder owner(User owner) {
            this.owner = owner;
            return this;
        }

        public Builder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder isPrivate(Boolean isPrivate) {
            this.isPrivate = isPrivate;
            return this;
        }

        public Builder isBanned(Boolean isBanned) {
            this.isBanned = isBanned;
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
