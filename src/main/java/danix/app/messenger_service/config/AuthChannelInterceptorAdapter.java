package danix.app.messenger_service.config;

import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.ChannelsRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.repositories.GroupsRepository;
import danix.app.messenger_service.repositories.UsersRepository;
import danix.app.messenger_service.security.JWTUtil;
import danix.app.messenger_service.services.TokensService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {
    private final JWTUtil jwtUtil;
    private final TokensService tokensService;
    private final UsersRepository usersRepository;
    private final ChannelsRepository channelsRepository;
    private final GroupsRepository groupsRepository;
    private final ChatsRepository chatsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AuthChannelInterceptorAdapter(JWTUtil jwtUtil, TokensService tokensService, UsersRepository usersRepository,
                                         ChannelsRepository channelsRepository, GroupsRepository groupsRepository,
                                         ChatsRepository chatsRepository, @Lazy SimpMessagingTemplate messagingTemplate) {
        this.jwtUtil = jwtUtil;
        this.tokensService = tokensService;
        this.usersRepository = usersRepository;
        this.channelsRepository = channelsRepository;
        this.groupsRepository = groupsRepository;
        this.chatsRepository = chatsRepository;
        this.messagingTemplate = messagingTemplate;
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
                    User user = usersRepository.findByEmail(email)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                    if (!tokensService.isValid(jwtUtil.getIdFromToken(token))) {
                        throw new IllegalArgumentException("Invalid token");
                    }
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),
                                    null,
                                    Collections.singleton(new SimpleGrantedAuthority(user.getRole().toString()))
                            );
                    accessor.setUser(authToken);
                }
            }
            case SUBSCRIBE -> {
                User user = null;
                try {
                    Authentication authentication = (Authentication) accessor.getUser();
                    String email = authentication.getPrincipal().toString();
                    user = usersRepository.findByEmail(email)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                    String dest = accessor.getDestination();
                    if (dest.startsWith("/topic/user")) {
                        if (!dest.contains(user.getWebSocketUUID())) {
                            throw new IllegalArgumentException("Invalid destination");
                        }
                    } else if (dest.startsWith("/topic/chat/")) {
                        String webSocketUUID = dest.substring(12);
                        Set<String> webSockets = chatsRepository.getWebSocketsByUser(user.getId());
                        if (!webSockets.contains(webSocketUUID)) {
                            throw new IllegalArgumentException("Invalid destination");
                        }
                    } else if (dest.startsWith("/topic/group/")) {
                        String webSocketUUID = dest.substring(13);
                        Set<String> webSockets = groupsRepository.getWebSocketByUserId(user.getId());
                        if (!webSockets.contains(webSocketUUID)) {
                            throw new IllegalArgumentException("Invalid destination");
                        }
                    } else if (dest.startsWith("/topic/channel/")) {
                        String webSocketUUID;
                        if (dest.contains("/post")) {
                            webSocketUUID = dest.substring(15, dest.indexOf('/', 15));
                        } else {
                            webSocketUUID = dest.substring(15);
                        }
                        Set<String> webSocketsUUIDS = channelsRepository.getWebSocketsByUser(user.getId());
                        if (!webSocketsUUIDS.contains(webSocketUUID)) {
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