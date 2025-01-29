package danix.app.messenger_service.dto;

import danix.app.messenger_service.models.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ResponseChannelPostDTO {
    private String text;
    private ResponseUserDTO owner;
    private int commentsCount;
    private int likes;
    private boolean isLiked;
    private long postId;
    private ContentType contentType;
    private List<ResponseChannelPostFilesDTO> files;
    private LocalDateTime createdAt;

    public static Builder builder() {
        return new Builder();
    }

    public ResponseChannelPostDTO(Builder builder) {
        this.text = builder.text;
        this.owner = builder.owner;
        this.commentsCount = builder.commentsCount;
        this.likes = builder.likes;
        this.postId = builder.id;
        this.contentType = builder.contentType;
        this.files = builder.files;
        this.createdAt = builder.createdAt;
        this.isLiked = builder.isLiked;
    }

    public static class Builder {
        private String text;
        private ResponseUserDTO owner;
        private int commentsCount;
        private int likes;
        private long id;
        private ContentType contentType;
        private List<ResponseChannelPostFilesDTO> files;
        private LocalDateTime createdAt;
        private boolean isLiked;

        public Builder images(List<ResponseChannelPostFilesDTO> images) {
            this.files = images;
            return this;
        }

        public Builder isLiked(boolean isLiked) {
            this.isLiked = isLiked;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder owner(ResponseUserDTO owner) {
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
