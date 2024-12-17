package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Channel;
import danix.app.messenger_service.models.ChannelInvite;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelsInvitesRepository extends JpaRepository<ChannelInvite, Long> {

    Optional<ChannelInvite> findByUserAndChannel(User user, Channel channel);

    List<ChannelInvite> findAllByUser(User user);

    @Modifying
    @Query("delete from ChannelInvite where expiredTime <= current_timestamp")
    void deleteExpiredInvites();
}
