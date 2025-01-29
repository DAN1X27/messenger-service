package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channels_posts_files")
@Data
@NoArgsConstructor
public class ChannelPostFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    private ChannelPost post;

    @Column(name = "file_uuid")
    private String fileUUID;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentType contentType;
}
