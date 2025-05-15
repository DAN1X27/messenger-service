package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "channels_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "post")
    private String text;

    @ManyToOne
    @JoinColumn(name = "channel_id", referencedColumnName = "id")
    private Channel channel;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private ChannelUser owner;

    @OneToMany(mappedBy = "post")
    private List<ChannelPostComment> comments;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentType contentType;

    @OneToMany(mappedBy = "post")
    private List<ChannelPostFile> files;

    @OneToMany(mappedBy = "post")
    private List<ChannelPostLike> likes;
}
