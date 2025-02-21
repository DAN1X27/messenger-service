package danix.app.messenger_service;

import danix.app.messenger_service.dto.ResponseChatMessageDTO;
import danix.app.messenger_service.dto.ResponseMessageUpdatingDTO;
import danix.app.messenger_service.dto.ResponseUserDTO;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BlockedUsersRepository;
import danix.app.messenger_service.repositories.ChatsMessagesRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.ChatsMessagesService;
import danix.app.messenger_service.util.MessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import util.TestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatsMessagesServiceTest {

    private final User currentUser = TestUtils.getTestCurrentUser();

    private final User testUser = TestUtils.getTestUser();

    private final Chat testChat = new Chat(currentUser, testUser, UUID.randomUUID().toString());

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BlockedUsersRepository blockedUsersRepository;

    @Mock
    private ChatsRepository chatsRepository;

    @Mock
    private ChatsMessagesRepository chatsMessagesRepository;

    @InjectMocks
    private ChatsMessagesService chatsMessagesService;

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
    }

    @Test
    public void sendMessage() {
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(modelMapper.map(currentUser, ResponseUserDTO.class)).thenReturn(new ResponseUserDTO());
        chatsMessagesService.sendTextMessage("Test message", testChat.getId());
        verify(chatsMessagesRepository, times(1)).save(any(ChatMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/" + testChat.getWebSocketUUID()),
                any(ResponseChatMessageDTO.class));
    }

    @Test
    public void sendMessageWhenCurrentUserBlockedByOtherUser() {
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(MessageException.class, () -> chatsMessagesService.sendTextMessage("Test message", testChat.getId()));
    }

    @Test
    public void sendMessageWhenOtherUserBlockedByCurrentUser() {
        when(chatsRepository.findById(testChat.getId())).thenReturn(Optional.of(testChat));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(MessageException.class, () -> chatsMessagesService.sendTextMessage("Test message", testChat.getId()));
    }

    @Test
    public void deleteMessage() {
        ChatMessage chatMessage = getChatMessage();
        when(chatsMessagesRepository.findById(1L)).thenReturn(Optional.of(chatMessage));
        chatsMessagesService.deleteMessage(1L);
        verify(chatsMessagesRepository, times(1)).delete(chatMessage);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/" + testChat.getWebSocketUUID()),
                any(Map.class));
    }

    @Test
    public void deleteMessageWhenMessageNotFound() {
        when(chatsMessagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(MessageException.class, () -> chatsMessagesService.deleteMessage(1L));
    }

    @Test
    public void deleteMessageWhenCurrentUserNotExistsInMessageChat() {
        ChatMessage chatMessage = getChatMessage();
        testChat.setUser1(testUser);
        chatMessage.setChat(testChat);
        when(chatsMessagesRepository.findById(1L)).thenReturn(Optional.of(chatMessage));
        assertThrows(MessageException.class, () -> chatsMessagesService.deleteMessage(1L));
        try {
            chatsMessagesService.deleteMessage(1L);
        } catch (MessageException e) {
            assertEquals("Curren user not exist in this chat.", e.getMessage());
        }
    }

    @Test
    public void deleteMessageWhenCurrentUserNotOwnerOfMessage() {
        ChatMessage chatMessage = getChatMessage();
        chatMessage.setOwner(testUser);
        when(chatsMessagesRepository.findById(1L)).thenReturn(Optional.of(chatMessage));
        assertThrows(MessageException.class, () -> chatsMessagesService.deleteMessage(1L));
        try {
            chatsMessagesService.deleteMessage(1L);
        } catch (MessageException e) {
            assertEquals("Current user not own this message.", e.getMessage());
        }
    }

    @Test
    public void updateMessage() {
        ChatMessage chatMessage = getChatMessage();
        when(chatsMessagesRepository.findById(1L)).thenReturn(Optional.of(chatMessage));
        chatsMessagesService.updateMessage(1L, "New message");
        assertEquals("New message", chatMessage.getText());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/" + testChat.getWebSocketUUID()),
                any(ResponseMessageUpdatingDTO.class));
    }

    @Test
    public void updateMessageWhenMessageIsImage() {
        ChatMessage chatMessage = getChatMessage();
        chatMessage.setContentType(ContentType.IMAGE);
        when(chatsMessagesRepository.findById(1L)).thenReturn(Optional.of(chatMessage));
        assertThrows(MessageException.class, () -> chatsMessagesService.updateMessage(1L, "New message"));
    }

    private ChatMessage getChatMessage() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(1L);
        chatMessage.setChat(testChat);
        chatMessage.setOwner(currentUser);
        chatMessage.setContentType(ContentType.TEXT);
        return chatMessage;
    }
}
