package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channels_posts_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelPostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "comment")
    private String text;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private ChannelUser owner;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    private ChannelPost post;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentType contentType;
}
