package danix.app.messenger_service;

import danix.app.messenger_service.dto.ResponseChatCreatedDTO;
import danix.app.messenger_service.dto.ResponseChatMessageDTO;
import danix.app.messenger_service.dto.ResponseUserDTO;
import danix.app.messenger_service.dto.ShowChatDTO;
import danix.app.messenger_service.models.BlockedUser;
import danix.app.messenger_service.models.Chat;
import danix.app.messenger_service.models.ChatMessage;
import danix.app.messenger_service.models.User;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatsServiceTest {

    private final User currentUser = TestUtils.getTestCurrentUser();

    private final User testUser = TestUtils.getTestUser();

    private final Chat testChat = new Chat(currentUser, testUser, UUID.randomUUID().toString());

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
    public void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
    }

    @Test
    public void createChat() {
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
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.of(new Chat()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void createChatWhenChatExistsByUser() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.empty());
        when(chatsRepository.findByUser1AndUser2(testUser, currentUser)).thenReturn(Optional.of(new Chat()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void createChatWhenCurrentUserBlockedByUser() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.empty());
        when(chatsRepository.findByUser1AndUser2(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void createChatWhenUserBlockedByCurrentUser() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        when(chatsRepository.findByUser1AndUser2(currentUser, testUser)).thenReturn(Optional.empty());
        when(chatsRepository.findByUser1AndUser2(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(ChatException.class, () -> chatsService.createChat(testUser.getId()));
    }

    @Test
    public void showChat() {
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
        ShowChatDTO showChatDTO = chatsService.showChat(testChat.getId(), 1, 1);
        assertNotNull(showChatDTO);
        assertEquals(testChat.getId(), showChatDTO.getId());
        assertEquals(testUser.getId(), showChatDTO.getUser().getId());
        Map<Long, ResponseChatMessageDTO> responseMessages = showChatDTO.getMessages().stream()
                .collect(Collectors.toMap(ResponseChatMessageDTO::getMessageId, Function.identity()));
        for (ChatMessage message : testChat.getMessages()) {
            ResponseChatMessageDTO chatMessage = responseMessages.get(message.getId());
            assertNotNull(chatMessage);
            assertEquals(message.getOwner().getId(), chatMessage.getSender().getId());
            if (message.getOwner().getId() == currentUser.getId()) {
                assertFalse(message.isRead());
            } else if (message.getOwner().getId() == testUser.getId()) {
                assertTrue(message.isRead());
            }
        }
    }

    @Test
    public void showChatWhenChatNotFound() {
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.showChat(testChat.getId(), 1, 1));
    }

    @Test
    public void showChatWhenCurrentUserNotExistsInChat() {
        User testChatUser = new User();
        testChatUser.setId(3);
        testChat.setUser1(testChatUser);
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        assertThrows(ChatException.class, () -> chatsService.showChat(testChat.getId(), 1, 1));
    }
}
