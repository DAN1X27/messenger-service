package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ChannelPost;
import danix.app.messenger_service.models.ChannelPostLike;
import danix.app.messenger_service.models.ChannelPostLikeKey;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelsPostsLikesRepository extends JpaRepository<ChannelPostLike, ChannelPostLikeKey> {
    Optional<ChannelPostLike> findByUserAndPost(User user, ChannelPost post);
}
