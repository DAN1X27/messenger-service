package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.User;
import danix.app.messenger_service.models.UserFriend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersFriendsRepository extends JpaRepository<UserFriend, Integer> {
    List<UserFriend> findByOwner(User owner);

    Optional<UserFriend> findByOwnerAndFriend(User owner, User friend);

    List<UserFriend> findByOwnerOrFriend(User owner, User friend);

    List<UserFriend> findByFriend(User friend);
}
