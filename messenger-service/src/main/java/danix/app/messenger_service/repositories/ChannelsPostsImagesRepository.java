package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ChannelPostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelsPostsImagesRepository extends JpaRepository<ChannelPostImage, Long> {
    Optional<ChannelPostImage> findByPostIdAndId(long postId, long imageId);
}
