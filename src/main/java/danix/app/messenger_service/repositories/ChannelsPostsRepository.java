package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Channel;
import danix.app.messenger_service.models.ChannelPost;
import danix.app.messenger_service.models.ChannelPostFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelsPostsRepository extends JpaRepository<ChannelPost, Long> {

    List<IdProjection> findAllByChannel(Channel channel, Pageable pageable);

    @Query("select p from ChannelPost p left join fetch p.files where p.id in (:ids)")
    List<ChannelPost> findAllByIdIn(List<Long> ids, Sort sort);

    @Modifying
    @Query("delete from ChannelPost p where p.id = :id")
    void deleteById(@Param("id") long id);
}
