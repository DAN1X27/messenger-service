package danix.app.messenger_service;

import danix.app.messenger_service.dto.ResponseChatCreatedDTO;
import danix.app.messenger_service.dto.ResponseChatMessageDTO;
import danix.app.messenger_service.dto.ResponseUserDTO;
import danix.app.messenger_service.dto.ShowChatDTO;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BlockedUsersRepository;
import danix.app.messenger_service.repositories.ChatsMessagesRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.ChatsService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.ChatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import util.TestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatsServiceTest {

    private final User currentUser = TestUtils.getTestCurrentUser();

    private final User testUser = TestUtils.getTestUser();

    private Chat testChat;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private BlockedUsersRepository blockedUsersRepository;

    @Mock
    private ChatsMessagesRepository chatsMessagesRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChatsRepository chatsRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatsService chatsService;

    @BeforeEach
    public void setTestChat() {
       testChat = new Chat(currentUser, testUser, UUID.randomUUID().toString());
    }

    @Test
    public void createChat() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.empty());
        when(chatsRepository.findByUser1AndUser2(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        chatsService.createChat(testUser.getId());
        verify(chatsRepository, times(1)).save(any(Chat.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" +
                testUser.getWebSocketUUID() + "/main"), any(ResponseChatCreatedDTO.class));
    }

    @Test
    public void createChatWhenChatExistsByCurrentUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.of(new Chat()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void createChatWhenChatExistsByUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.empty());
        when(chatsRepository.findByUser1AndUser2(testUser, currentUser)).thenReturn(Optional.of(new Chat()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void createChatWhenCurrentUserBlockedByUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.empty());
        when(chatsRepository.findByUser1AndUser2(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void createChatWhenUserBlockedByCurrentUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.empty());
        when(chatsRepository.findByUser1AndUser2(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void showChat() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        ChatMessage testMessage1 = new ChatMessage();
        testMessage1.setId(1);
        testMessage1.setText("testMessage1");
        testMessage1.setOwner(currentUser);
        ChatMessage testMessage2 = new ChatMessage();
        testMessage2.setId(2);
        testMessage2.setText("testMessage2");
        testMessage2.setOwner(currentUser);
        ChatMessage testMessage3 = new ChatMessage();
        testMessage3.setId(3);
        testMessage3.setText("testMessage3");
        testMessage3.setOwner(testUser);
        testChat.setMessages(List.of(testMessage1, testMessage2, testMessage3));
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        when(chatsMessagesRepository.findAllByChat(testChat,
                PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id"))))
                    .thenReturn(List.of(testMessage1, testMessage2, testMessage3));
        ResponseUserDTO respUser1 = new ResponseUserDTO();
        respUser1.setId(testUser.getId());
        when(modelMapper.map(testUser, ResponseUserDTO.class)).thenReturn(respUser1);
        ResponseUserDTO respUser2 = new ResponseUserDTO();
        respUser2.setId(currentUser.getId());
        when(modelMapper.map(currentUser, ResponseUserDTO.class)).thenReturn(respUser2);
        when(modelMapper.map(any(), eq(ResponseChatMessageDTO.class))).thenReturn(new ResponseChatMessageDTO());
        ShowChatDTO showChatDTO = chatsService.showChat(testChat.getId(), 1, 1);
        assertNotNull(showChatDTO);
        assertEquals(testChat.getId(), showChatDTO.getId());
        assertEquals(testUser.getId(), showChatDTO.getUser().getId());
    }

    @Test
    public void showChatWhenChatNotFound() {
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.showChat(testChat.getId(), 1, 1));
    }

    @Test
    public void showChatWhenCurrentUserNotExistsInChat() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        User testChatUser = new User();
        testChatUser.setId(3);
        testChat.setUser1(testChatUser);
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        assertThrows(ChatException.class, () -> chatsService.showChat(testChat.getId(), 1, 1));
    }

    @Test
    public void deleteChat() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        when(chatsMessagesRepository.findAllByChatAndContentTypeIsNot(eq(testChat), eq(ContentType.TEXT), any()))
                .thenReturn(Collections.emptyList());
        CompletableFuture<Void> future = chatsService.deleteChat(testChat.getId());
        future.join();
        verify(messagingTemplate).convertAndSend("/topic/chat/" + testChat.getWebSocketUUID(),
                Map.of("deleted", true));
        verify(chatsRepository).deleteById(testChat.getId());
    }

    @Test
    public void deleteChatWhenChatNotFound() {
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.deleteChat(testChat.getId()));
    }

    @Test
    public void deleteChatWhenUserNotExistsInChat() {
        testChat.setUser1(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        assertThrows(ChatException.class, () -> chatsService.deleteChat(testChat.getId()));
    }
}
