package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Group;
import danix.app.messenger_service.models.GroupActionMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupsActionsMessagesRepository extends JpaRepository<GroupActionMessage, Long> {
    List<GroupActionMessage> findAllByGroup(Group group, Pageable pageable);
}
