package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ChannelsRepository extends JpaRepository<Channel, Integer> {
    Optional<Channel> findByName(String name);

    Optional<Channel> findByNameStartsWith(String name);

    @Query("select c.webSocketUUID from Channel c left join c.users cu where cu.user.id = :id")
    Set<String> getWebSocketsByUser(@Param("id") Integer id);
}
