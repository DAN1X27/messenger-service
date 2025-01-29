package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ChannelPost;
import danix.app.messenger_service.models.ChannelPostComment;
import danix.app.messenger_service.models.ContentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelsPostsCommentsRepository extends JpaRepository<ChannelPostComment, Long> {

    List<ChannelPostComment> findAllByPostIn(List<ChannelPost> posts);

    List<ChannelPostComment> findAllByPost(ChannelPost post, Pageable pageable);
}
