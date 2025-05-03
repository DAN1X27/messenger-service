package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupsRepository extends JpaRepository<Group, Integer> {

    @Query(
            """
               select exists (select g.id from Group g left join g.users gu on gu.group.id = g.id
                             where g.webSocketUUID = :web_socket_uuid and gu.user.id = :user_id)
            """
    )
    boolean existsByWebSocketUUIDAndUserId(@Param("web_socket_uuid") String webSocketUUID, @Param("user_id") Integer userId);

    @Modifying
    @Query("delete from Group g where g.id = :id")
    void deleteById(@Param("id") int id);

}
