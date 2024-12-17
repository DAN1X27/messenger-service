package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Group;
import danix.app.messenger_service.models.GroupInvite;
import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupsInvitesRepository extends JpaRepository<GroupInvite, Integer> {
    Optional<GroupInvite> findByGroupAndUser(Group group, User user);

    List<GroupInvite> findByUser(User user);

    @Modifying
    @Query("delete from GroupInvite where expiredTime <= current_timestamp")
    void deleteExpiredInvites();
}
