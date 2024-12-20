package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ShowChannelDTO {
    private int id;
    private String name;
    private List<ResponseChannelUserDTO> users;
    private List<ResponseChannelPostDTO> posts;
    private Date createdAt;
    private ResponseUserDTO owner;
    private String description;
    private Boolean isPrivate;

    public static Builder builder() {
        return new Builder();
    }

    public ShowChannelDTO(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.users = builder.users;
        this.posts = builder.posts;
        this.createdAt = builder.createdAt;
        this.owner = builder.owner;
        this.description = builder.description;
        this.isPrivate = builder.isPrivate;
    }

    public static class Builder {
        private int id;
        private String name;
        private List<ResponseChannelUserDTO> users;
        private List<ResponseChannelPostDTO> posts;
        private Date createdAt;
        private ResponseUserDTO owner;
        private String description;
        private boolean isPrivate;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder users(List<ResponseChannelUserDTO> users) {
            this.users = users;
            return this;
        }

        public Builder posts(List<ResponseChannelPostDTO> posts) {
            this.posts = posts;
            return this;
        }

        public Builder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder owner(ResponseUserDTO owner) {
            this.owner = owner;
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

        public ShowChannelDTO build() {
            return new ShowChannelDTO(this);
        }
    }
}
