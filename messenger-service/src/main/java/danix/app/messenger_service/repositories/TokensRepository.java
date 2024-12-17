package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.User;
import danix.app.messenger_service.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokensRepository extends JpaRepository<Token, String> {
    List<Token> findByOwner(User owner);

    @Modifying
    @Query("delete from Token where expiredDate <= current_date or status='REVOKED'")
    void deleteExpiredTokens();
}
