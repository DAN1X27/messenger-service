package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "groups")
@Data
@NoArgsConstructor
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

    @OneToMany(mappedBy = "group")
    private List<GroupBannedUser> bannedUsers;

    @OneToMany(mappedBy = "group")
    private List<GroupActionMessage> actionMessages;

    @Column(name = "image")
    private String image;

    private String description;

    public static Builder builder() {
        return new Builder();
    }

    public Group(Builder builder) {
        this.name = builder.name;
        this.owner = builder.owner;
        this.createdAt = builder.createdAt;
        this.description = builder.description;
        this.image = builder.image;
    }

    public static class Builder {
        private String name;
        private User owner;
        private Date createdAt;
        private String description;
        private String image;

        public Builder name(String name) {
            this.name = name;
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

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Group build() {
            return new Group(this);
        }
    }
}
