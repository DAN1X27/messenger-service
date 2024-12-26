package danix.app.messenger_service.config;

import danix.app.messenger_service.util.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {
    private static final String TOKEN_HEADER = "token";
    private final WebSocketAuthenticatorService authenticatorService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor.getCommand() == StompCommand.CONNECT) {
            try {
                String token = accessor.getFirstNativeHeader(TOKEN_HEADER);
                UsernamePasswordAuthenticationToken user = authenticatorService.authenticate(token);
                accessor.setUser(user);
            } catch (AuthenticationException e) {
                messagingTemplate.convertAndSend("/topic/user/errors",
                        "Authentication failed: " + e.getMessage());
            }
        }
        return message;
    }
}
