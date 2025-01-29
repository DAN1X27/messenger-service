package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TokensRepository extends JpaRepository<Token, String> {

    @Modifying
    @Query("delete from Token where expiredDate <= current_date or status='REVOKED'")
    void deleteExpiredTokens();

    @Modifying
    @Query("update Token set status='REVOKED' where owner.id = :id")
    void banUserTokens(@Param("id") Integer id);
}
