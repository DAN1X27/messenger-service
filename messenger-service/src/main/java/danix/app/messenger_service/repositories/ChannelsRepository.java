package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelsRepository extends JpaRepository<Channel, Integer> {
    Optional<Channel> findByName(String name);

    Optional<Channel> findByNameStartsWith(String name);
}
