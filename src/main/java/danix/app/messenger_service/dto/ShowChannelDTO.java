package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ShowChannelDTO {
    private int id;
    private String name;
    private String description;
    private List<ResponseChannelPostDTO> posts;
    @JsonProperty("users_count")
    private int usersCount;
    @JsonProperty("created_at")
    private Date createdAt;
    private ResponseUserDTO owner;
    @JsonProperty("web_socket")
    private String webSocketUUID;

    public static Builder builder() {
        return new Builder();
    }

    public ShowChannelDTO(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.posts = builder.posts;
        this.createdAt = builder.createdAt;
        this.owner = builder.owner;
        this.description = builder.description;
        this.webSocketUUID = builder.webSocketUUID;
        this.usersCount = builder.usersCount;
    }

    public static class Builder {
        private int id;
        private String name;
        private List<ResponseChannelPostDTO> posts;
        private Date createdAt;
        private ResponseUserDTO owner;
        private String description;
        private String webSocketUUID;
        private int usersCount;

        public Builder usersCount(int usersCount) {
            this.usersCount = usersCount;
            return this;
        }

        public Builder webSocketUUID(String webSocketUUID) {
            this.webSocketUUID = webSocketUUID;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder id(int id) {
            this.id = id;
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

        public ShowChannelDTO build() {
            return new ShowChannelDTO(this);
        }
    }
}
