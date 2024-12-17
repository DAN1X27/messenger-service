package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.BlockedUser;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedUsersRepository extends JpaRepository<BlockedUser, Integer> {
    Optional<BlockedUser> findByOwnerAndBlockedUser(User owner, User blockedUser);
}
