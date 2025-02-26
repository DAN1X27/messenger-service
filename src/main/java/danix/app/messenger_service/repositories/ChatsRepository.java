package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.User;
import danix.app.messenger_service.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatsRepository extends JpaRepository<Chat, Integer> {
    Optional<Chat> findByUser1AndUser2(User user1, User user2);

    List<Chat> findByUser1OrUser2(User user1, User user2);

    @Query(
            """
            select exists (select c.id from Chat c where (c.user1.id = :user_id or c.user2.id = :user_id)
                        and c.webSocketUUID = :web_socket_uuid)
            """
    )
    boolean existByWebSocketUUIDAndUserId(@Param("web_socket_uuid") String webSocketUUID, @Param("user_id") Integer userId);
}
