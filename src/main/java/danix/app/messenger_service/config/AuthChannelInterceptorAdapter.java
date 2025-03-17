package danix.app.messenger_service.config;

import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.ChannelsRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.repositories.GroupsRepository;
import danix.app.messenger_service.security.JWTUtil;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.security.UserDetailsServiceImpl;
import danix.app.messenger_service.services.TokensService;
import danix.app.messenger_service.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {
    private final JWTUtil jwtUtil;
    private final TokensService tokensService;
    private final ChannelsRepository channelsRepository;
    private final GroupsRepository groupsRepository;
    private final ChatsRepository chatsRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthChannelInterceptorAdapter(JWTUtil jwtUtil, TokensService tokensService,
                                         ChannelsRepository channelsRepository, GroupsRepository groupsRepository,
                                         ChatsRepository chatsRepository, @Lazy SimpMessagingTemplate messagingTemplate,
                                         UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.tokensService = tokensService;
        this.channelsRepository = channelsRepository;
        this.groupsRepository = groupsRepository;
        this.chatsRepository = chatsRepository;
        this.messagingTemplate = messagingTemplate;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        switch (accessor.getCommand()) {
            case CONNECT -> {
                String token = accessor.getFirstNativeHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    String email = jwtUtil.validateTokenAndRetrieveClaim(token);
                    UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);
                    if (!tokensService.isValid(jwtUtil.getIdFromToken(token))) {
                        throw new IllegalArgumentException("Invalid token");
                    }
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    userDetails.getPassword(),
                                    userDetails.getAuthorities()
                            );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    accessor.setUser(authToken);
                }
            }
            case SUBSCRIBE -> {
                User user = null;
                try {
                    Authentication authentication = (Authentication) accessor.getUser();
                    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                    user = userDetails.getUser();
                    String dest = accessor.getDestination();
                    int id = user.getId();
                    if (dest.startsWith("/topic/user")) {
                        if (!dest.contains(user.getWebSocketUUID())) {
                            throw new IllegalArgumentException("Invalid destination");
                        }
                    } else if (dest.startsWith("/topic/chat/")) {
                        String webSocketUUID = dest.substring(12);
                        if (!chatsRepository.existByWebSocketUUIDAndUserId(webSocketUUID, id)) {
                            throw new IllegalArgumentException("Invalid destination");
                        }
                    } else if (dest.startsWith("/topic/group/")) {
                        String webSocketUUID = dest.substring(13);
                        if (!groupsRepository.existsByWebSocketUUIDAndUserId(webSocketUUID, id)) {
                            throw new IllegalArgumentException("Invalid destination");
                        }
                    } else if (dest.startsWith("/topic/channel/")) {
                        String webSocketUUID;
                        if (dest.contains("/post")) {
                            webSocketUUID = dest.substring(15, dest.indexOf('/', 15));
                        } else {
                            webSocketUUID = dest.substring(15);
                        }
                        if (!channelsRepository.existsByWebSocketUUIDAndUserId(webSocketUUID, id)) {
                            throw new IllegalArgumentException("Invalid destination");
                        }
                    } else if (!dest.equals("/topic/user/" + user.getWebSocketUUID() + "/errors")){
                        throw new IllegalArgumentException("Invalid destination");
                    }
                } catch (Exception e) {
                    if (user != null) {
                        messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/errors",
                                Map.of("error", e.getMessage()));
                    }
                    throw e;
                }
            }
        }
        return message;
    }
}