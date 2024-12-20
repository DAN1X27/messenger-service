package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ChannelPostComment;
import danix.app.messenger_service.models.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelsPostsCommentsRepository extends JpaRepository<ChannelPostComment, Long> {
    List<ChannelPostComment> findAllByPostIdAndContentType(Long postId, ContentType contentType);
}
