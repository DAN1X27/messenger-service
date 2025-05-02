package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelsRepository extends JpaRepository<Channel, Integer> {
    Optional<Channel> findByName(String name);

    Optional<Channel> findByNameStartsWith(String name);

    @Query(
            """
                select exists (select c.id from Channel c left join c.users cu on c.id = cu.channel.id
                               where c.webSocketUUID = :web_socket_uuid and cu.user.id = :user_id)
            """
    )
    boolean existsByWebSocketUUIDAndUserId(@Param("web_socket_uuid") String webSocketUUID, @Param("user_id") Integer userId);

    @Modifying
    @Query("delete from Channel c where c.id = :id")
    void deleteById(@Param("id") int id);

}
