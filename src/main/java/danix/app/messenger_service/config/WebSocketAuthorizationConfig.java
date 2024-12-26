package danix.app.messenger_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketAuthorizationConfig {

    @Bean
    public AuthorizationManager<Message<?>> configureInbound(MessageMatcherDelegatingAuthorizationManager.Builder authorizationManager) {
       return authorizationManager
               .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.DISCONNECT, SimpMessageType.UNSUBSCRIBE).permitAll()
               .simpDestMatchers("/topic/user/errors").permitAll()
               .anyMessage().hasAnyRole("USER", "ADMIN")
               .build();
    }
}
