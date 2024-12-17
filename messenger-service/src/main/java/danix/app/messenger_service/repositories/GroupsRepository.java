package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupsRepository extends JpaRepository<Group, Integer> {

}
