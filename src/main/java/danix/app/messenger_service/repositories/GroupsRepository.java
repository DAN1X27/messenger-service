package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface GroupsRepository extends JpaRepository<Group, Integer> {

    @Query("select g.webSocketUUID from Group g left join g.users gu where gu.user.id = :id")
    Set<String> getWebSocketByUserId(@Param("id") Integer id);
}
