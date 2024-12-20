package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ShowGroupDTO {
    private int groupId;
    private String groupName;
    private ResponseUserDTO owner;
    private List<ResponseGroupMessageDTO> messages;
    private List<ResponseGroupUserDTO> users;
    private List<ResponseGroupActionMessageDTO> groupActionMessages;
    private Date createdAt;
    private int usersCount;
    private String description;

    public static Builder builder() {
        return new Builder();
    }

    public ShowGroupDTO(Builder builder) {
        this.groupId = builder.groupId;
        this.groupName = builder.groupName;
        this.owner = builder.owner;
        this.messages = builder.messages;
        this.users = builder.users;
        this.groupActionMessages = builder.groupActionMessages;
        this.createdAt = builder.createdAt;
        this.usersCount = builder.usersCount;
        this.description = builder.description;
    }

    public static class Builder {
        private int groupId;
        private String groupName;
        private ResponseUserDTO owner;
        private List<ResponseGroupMessageDTO> messages;
        private List<ResponseGroupUserDTO> users;
        private List<ResponseGroupActionMessageDTO> groupActionMessages;
        private Date createdAt;
        private int usersCount;
        private String description;

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

        public Builder users(List<ResponseGroupUserDTO> users) {
            this.users = users;
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
