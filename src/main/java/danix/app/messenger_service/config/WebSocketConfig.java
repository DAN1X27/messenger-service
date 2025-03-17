package danix.app.messenger_service.config;
import danix.app.messenger_service.models.User;
import danix.app.messenger_service.security.JWTUtil;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static danix.app.messenger_service.services.UserService.getCurrentUser;
import static org.springframework.messaging.simp.SimpMessageType.*;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptorAdapter authChannelInterceptorAdapter;

    @Value("${client_url}")
    private String url;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(url).withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptorAdapter, csrfInterceptor());
    }

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        return messages
                .simpTypeMatchers(CONNECT, DISCONNECT, UNSUBSCRIBE).permitAll()
                .anyMessage().authenticated()
                .build();
    }

    @Bean(name = "csrfChannelInterceptor")
    ChannelInterceptor csrfInterceptor() {
        return new ChannelInterceptor() {};
    }
}
