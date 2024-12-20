package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ChatMessage;
import danix.app.messenger_service.models.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatsMessagesRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByChat(Chat chat, Pageable pageable);
}
