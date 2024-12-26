package danix.app.messenger_service.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import danix.app.messenger_service.models.User;
import danix.app.messenger_service.repositories.UsersRepository;
import danix.app.messenger_service.security.JWTUtil;
import danix.app.messenger_service.services.TokensService;
import danix.app.messenger_service.util.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class WebSocketAuthenticatorService {
    private final TokensService tokensService;
    private final JWTUtil jwtUtil;
    private final UsersRepository usersRepository;

    public UsernamePasswordAuthenticationToken authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Token cannot be empty");
        }
        String tokenId = jwtUtil.getIdFromToken(token);
        try {
            if (!tokensService.isValid(tokenId)) {
                throw new AuthenticationException("Invalid token");
            }
            String email = jwtUtil.validateTokenAndRetrieveClaim(token);
            User user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new AuthenticationException("Invalid token"));
            return new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singleton((GrantedAuthority) () -> user.getRole().toString())
            );
        } catch (JWTVerificationException e) {
            throw new AuthenticationException("Invalid token");
        } catch (IllegalStateException e) {
            throw new AuthenticationException(e.getMessage());
        }
    }
}
