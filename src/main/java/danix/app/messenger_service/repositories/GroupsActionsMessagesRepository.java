package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.GroupActionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupsActionsMessagesRepository extends JpaRepository<GroupActionMessage, Long> {
}
