package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ChannelPost;
import danix.app.messenger_service.models.ChannelPostFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelsPostsFilesRepository extends JpaRepository<ChannelPostFile, Long> {
    List<ChannelPostFile> findAllByPostIn(List<ChannelPost> posts);
}
