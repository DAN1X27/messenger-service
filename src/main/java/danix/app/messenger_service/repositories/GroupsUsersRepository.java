package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Group;
import danix.app.messenger_service.models.GroupUser;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupsUsersRepository extends JpaRepository<GroupUser, Integer> {
    List<GroupUser> findAllByUser(User user);

    Optional<GroupUser> findByGroupAndUser(Group group, User user);

    List<GroupUser> findAllByGroup(Group group);
}
