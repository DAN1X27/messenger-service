package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.ContentType;
import danix.app.messenger_service.models.Group;
import danix.app.messenger_service.models.GroupMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupsMessagesRepository extends JpaRepository<GroupMessage, Long> {

    List<GroupMessage> findAllByGroup(Group group, Pageable pageable);
}
