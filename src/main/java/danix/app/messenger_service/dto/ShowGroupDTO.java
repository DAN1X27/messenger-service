package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ShowGroupDTO {
    private int id;
    private String name;
    private ResponseUserDTO owner;
    private List<ResponseGroupMessageDTO> messages;
    @JsonProperty("action_messages")
    private List<ResponseGroupActionMessageDTO> actionMessages;
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonProperty("users_count")
    private int usersCount;
    @JsonProperty("description")
    private String description;
    @JsonProperty("web_socket")
    private String webSocketUUID;

    public static Builder builder() {
        return new Builder();
    }

    public ShowGroupDTO(Builder builder) {
        this.id = builder.groupId;
        this.name = builder.groupName;
        this.owner = builder.owner;
        this.messages = builder.messages;
        this.actionMessages = builder.groupActionMessages;
        this.createdAt = builder.createdAt;
        this.usersCount = builder.usersCount;
        this.description = builder.description;
        this.webSocketUUID = builder.webSocketUUID;
    }

    public static class Builder {
        private int groupId;
        private String groupName;
        private ResponseUserDTO owner;
        private List<ResponseGroupMessageDTO> messages;
        private List<ResponseGroupActionMessageDTO> groupActionMessages;
        private Date createdAt;
        private int usersCount;
        private String description;
        private String webSocketUUID;

        public Builder webSocketUUID(String webSocketUUID) {
            this.webSocketUUID = webSocketUUID;
            return this;
        }

        public Builder name(String name) {
            this.groupName = name;
            return this;
        }

        public Builder owner(ResponseUserDTO owner) {
            this.owner = owner;
            return this;
        }

        public Builder messages(List<ResponseGroupMessageDTO> messages) {
            this.messages = messages;
            return this;
        }

        public Builder groupActionMessages(List<ResponseGroupActionMessageDTO> groupActionMessages) {
            this.groupActionMessages = groupActionMessages;
            return this;
        }

        public Builder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder usersCount(int usersCount) {
            this.usersCount = usersCount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder id(int groupId) {
            this.groupId = groupId;
            return this;
        }

        public ShowGroupDTO build() {
            return new ShowGroupDTO(this);
        }
    }
}
