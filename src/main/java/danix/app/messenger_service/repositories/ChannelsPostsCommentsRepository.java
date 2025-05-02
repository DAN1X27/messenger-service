package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ChannelPost;
import danix.app.messenger_service.models.ChannelPostComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelsPostsCommentsRepository extends JpaRepository<ChannelPostComment, Long> {

    List<ChannelPostComment> findAllByPost(ChannelPost post, Pageable pageable);
}
