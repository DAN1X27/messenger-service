package danix.app.messenger_service.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channels_posts_images")
@Data
@NoArgsConstructor
public class ChannelPostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    private ChannelPost post;

    @Column(name = "image_uuid")
    private String imageUUID;
}
