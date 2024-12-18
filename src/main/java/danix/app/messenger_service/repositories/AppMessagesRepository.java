package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.AppMessage;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppMessagesRepository extends JpaRepository<AppMessage, Long> {
    List<AppMessage> findByUser(User user);

    @Modifying
    @Query("delete from AppMessage where removeDate <= current_timestamp")
    void deleteExpiredMessages();
}
