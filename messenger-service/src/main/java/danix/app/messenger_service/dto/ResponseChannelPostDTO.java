package danix.app.messenger_service.dto;

import danix.app.messenger_service.models.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResponseChannelPostDTO {
    private String text;
    private String owner;
    private int commentsCount;
    private int likes;
    private boolean liked;
    private long id;
    private ContentType contentType;
    private List<ResponseChannelPostImageDTO> images;

    public static Builder builder() {
        return new Builder();
    }

    public ResponseChannelPostDTO(Builder builder) {
        this.text = builder.text;
        this.owner = builder.owner;
        this.commentsCount = builder.commentsCount;
        this.likes = builder.likes;
        this.id = builder.id;
        this.contentType = builder.contentType;
        this.images = builder.images;
    }

    public static class Builder {
        private String text;
        private String owner;
        private int commentsCount;
        private int likes;
        private long id;
        private ContentType contentType;
        private List<ResponseChannelPostImageDTO> images;

        public Builder images(List<ResponseChannelPostImageDTO> images) {
            this.images = images;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder commentsCount(int commentsCount) {
            this.commentsCount = commentsCount;
            return this;
        }

        public Builder likes(int likes) {
            this.likes = likes;
            return this;
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public ResponseChannelPostDTO build() {
            return new ResponseChannelPostDTO(this);
        }
    }
}
