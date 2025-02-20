package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseChatMessageDTO {
    private String message;
    private ResponseUserDTO sender;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
    @JsonProperty("message_id")
    private long messageId;
    @JsonProperty("read")
    private boolean isRead;
    private ContentType type;

    public ResponseChatMessageDTO(Builder builder) {
        this.message = builder.message;
        this.sender = builder.sender;
        this.sentTime = builder.sentTime;
        this.messageId = builder.messageId;
        this.isRead = builder.isRead;
        this.type = builder.type;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder{
        private String message;
        private ResponseUserDTO sender;
        private LocalDateTime sentTime;
        private long messageId;
        private boolean isRead;
        private ContentType type;

        public Builder message(String message){
            this.message = message;
            return this;
        }

        public Builder sender(ResponseUserDTO sender){
            this.sender = sender;
            return this;
        }

        public Builder sentTime(LocalDateTime sentTime){
            this.sentTime = sentTime;
            return this;
        }

        public Builder messageId(long messageId){
            this.messageId = messageId;
            return this;
        }

        public Builder isRead(boolean isRead){
            this.isRead = isRead;
            return this;
        }

        public Builder type(ContentType type){
            this.type = type;
            return this;
        }

        public ResponseChatMessageDTO build(){
            return new ResponseChatMessageDTO(this);
        }
    }
}
