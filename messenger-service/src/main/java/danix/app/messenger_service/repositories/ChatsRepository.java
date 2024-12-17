package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.User;
import danix.app.messenger_service.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatsRepository extends JpaRepository<Chat, Integer> {
    Optional<Chat> findByUser1AndUser2(User user1, User user2);

    List<Chat> findByUser1OrUser2(User user1, User user2);
}
