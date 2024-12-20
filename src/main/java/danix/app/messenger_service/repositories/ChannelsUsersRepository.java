package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Channel;
import danix.app.messenger_service.models.ChannelUser;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelsUsersRepository extends JpaRepository<ChannelUser, Integer> {
    List<ChannelUser> findAllByUser(User user);

    Optional<ChannelUser> findByUserAndChannel(User user, Channel channel);
}