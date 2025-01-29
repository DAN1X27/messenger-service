package danix.app.messenger_service.services;

import danix.app.messenger_service.models.User;
import danix.app.messenger_service.models.Token;
import danix.app.messenger_service.repositories.TokensRepository;
import danix.app.messenger_service.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Date;

import static danix.app.messenger_service.models.Token.Status.ISSUED;
import static danix.app.messenger_service.models.Token.Status.REVOKED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokensService {
    private final TokensRepository tokensRepository;
    private final JWTUtil jwtUtil;

    public boolean isValid(String id) {
        return tokensRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Invalid token"))
                .getStatus() != REVOKED;
    }

    @Transactional
    public void banUserTokens(Integer userId) {
        tokensRepository.banUserTokens(userId);
    }

    @Transactional
    public void create(String token, User owner) {
        Token tokenToSave = new Token();
        tokenToSave.setId(jwtUtil.getIdFromToken(token));
        tokenToSave.setStatus(ISSUED);
        tokenToSave.setOwner(owner);
        tokenToSave.setExpiredDate(Date.from(ZonedDateTime.now().plusDays(14).toInstant()));
        tokensRepository.save(tokenToSave);
    }
}
