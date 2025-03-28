package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Modifying
    void deleteAllByUserStatusAndCreatedAtBefore(User.Status userStatus, LocalDateTime createdAt);

    List<User> findAllByOnlineStatusAndLastOnlineStatusUpdateBefore(User.OnlineStatus userStatus, LocalDateTime createdAt);

    @Modifying
    @Query("""
        update User u set u.onlineStatus = 'OFFLINE', u.lastOnlineStatusUpdate = current_timestamp
        where u in :users
        """)
    void updateOnlineStatus(@Param("users") List<User> users);
}
