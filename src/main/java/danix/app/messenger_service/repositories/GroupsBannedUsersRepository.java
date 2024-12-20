package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.BannedGroupUserKey;
import danix.app.messenger_service.models.Group;
import danix.app.messenger_service.models.GroupBannedUser;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupsBannedUsersRepository extends JpaRepository<GroupBannedUser, BannedGroupUserKey> {
    Optional<GroupBannedUser> findByGroupAndUser(Group group, User user);
}
