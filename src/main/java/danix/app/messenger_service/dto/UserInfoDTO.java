package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserInfoDTO {
    private String username;
    private String email;
    private List<ResponseAppMessageDTO> appMessages;
    private List<ResponseChatDTO> chats;
    private List<ResponseGroupDTO> groups;
    private List<ResponseGroupInviteDTO> groupInvites;
    private List<ResponseChannelDTO> channels;
    private List<ResponseChannelInviteDTO> channelInvites;
    private List<ResponseUserDTO> friends;

    public static Builder builder() {
        return new Builder();
    }

    public UserInfoDTO(Builder builder) {
        this.username = builder.username;
        this.email = builder.email;
        this.appMessages = builder.appMessages;
        this.chats = builder.chats;
        this.groups = builder.groups;
        this.groupInvites = builder.groupInvites;
        this.channels = builder.channels;
        this.channelInvites = builder.channelInvites;
        this.friends = builder.friends;
    }

    public static class Builder {
        private String username;
        private String email;
        private List<ResponseAppMessageDTO> appMessages;
        private List<ResponseChatDTO> chats;
        private List<ResponseGroupDTO> groups;
        private List<ResponseGroupInviteDTO> groupInvites;
        private List<ResponseChannelDTO> channels;
        private List<ResponseChannelInviteDTO> channelInvites;
        private List<ResponseUserDTO> friends;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder appMessages(List<ResponseAppMessageDTO> appMessages) {
            this.appMessages = appMessages;
            return this;
        }

        public Builder chats(List<ResponseChatDTO> chats) {
            this.chats = chats;
            return this;
        }

        public Builder groups(List<ResponseGroupDTO> groups) {
            this.groups = groups;
            return this;
        }

        public Builder groupInvites(List<ResponseGroupInviteDTO> groupInvites) {
            this.groupInvites = groupInvites;
            return this;
        }

        public Builder channels(List<ResponseChannelDTO> channels) {
            this.channels = channels;
            return this;
        }

        public Builder channelInvites(List<ResponseChannelInviteDTO> channelInvites) {
            this.channelInvites = channelInvites;
            return this;
        }

        public Builder friends(List<ResponseUserDTO> friends) {
            this.friends = friends;
            return this;
        }

        public UserInfoDTO build() {
            return new UserInfoDTO(this);
        }
    }
}
