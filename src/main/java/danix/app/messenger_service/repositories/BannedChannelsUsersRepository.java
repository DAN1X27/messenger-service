package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.BannedChannelUser;
import danix.app.messenger_service.models.BannedChannelUserKey;
import danix.app.messenger_service.models.Channel;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BannedChannelsUsersRepository extends JpaRepository<BannedChannelUser, BannedChannelUserKey>{
    Optional<BannedChannelUser> findByUserAndChannel(User user, Channel channel);
}
