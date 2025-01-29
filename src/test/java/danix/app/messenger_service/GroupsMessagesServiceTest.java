package danix.app.messenger_service;

import danix.app.messenger_service.dto.ResponseGroupMessageDTO;
import danix.app.messenger_service.dto.ResponseMessageDeletionDTO;
import danix.app.messenger_service.dto.ResponseMessageUpdatingDTO;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.GroupsMessagesRepository;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.GroupsMessagesService;
import danix.app.messenger_service.services.GroupsService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupsMessagesServiceTest {

    private final User currentUser = TestUtils.getTestCurrentUser();

    private final User testUser = TestUtils.getTestUser();

    private final Group testGroup = TestUtils.getTestGroup();

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GroupsService groupsService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private GroupsMessagesRepository messagesRepository;

    @InjectMocks
    private GroupsMessagesService groupsMessagesService;

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
    }

    @Test
    public void sendTextMessage() {
        when(groupsService.getById(testGroup.getId())).thenReturn(testGroup);
        when(groupsService.getGroupUser(testGroup, currentUser)).thenReturn(new GroupUser());
        groupsMessagesService.sendTextMessage("test message", testGroup.getId());
        verify(messagesRepository, times(1)).save(any(GroupMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/0"),
                any(ResponseGroupMessageDTO.class));
    }

    @Test
    public void deleteMessageWhenCurrentUserOwnerOfMessage() {
        GroupMessage testMessage = getGroupMessage();
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        when(groupsService.getGroupUser(testGroup, currentUser)).thenReturn(new GroupUser());
        groupsMessagesService.deleteMessage(testMessage.getId());
        verify(messagesRepository, times(1)).delete(testMessage);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/0"),
                any(ResponseMessageDeletionDTO.class));
    }

    @Test
    public void deleteMessageWhenCurrentUserNotOwnerOfMessageButGroupAdmin() {
        GroupMessage testMessage = getGroupMessage();
        testMessage.setMessageOwner(testUser);
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        GroupUser testGroupUser = new GroupUser();
        testGroupUser.setAdmin(true);
        when(groupsService.getGroupUser(testGroup, currentUser)).thenReturn(testGroupUser);
        groupsMessagesService.deleteMessage(testMessage.getId());
        verify(messagesRepository, times(1)).delete(testMessage);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/0"),
                any(ResponseMessageDeletionDTO.class));
    }

    @Test
    public void deleteMessageWhenMessageNotFound() {
        when(messagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(MessageException.class, () -> groupsMessagesService.deleteMessage(1L));
    }

    @Test
    public void deleteMessageWhenCurrentUserNotOwnerOfMessageAndNotAdmin() {
        GroupMessage testMessage = getGroupMessage();
        testMessage.setMessageOwner(testUser);
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        when(groupsService.getGroupUser(testGroup, currentUser)).thenReturn(new GroupUser());
        assertThrows(MessageException.class, () -> groupsMessagesService.deleteMessage(testMessage.getId()));
    }

    @Test
    public void updateMessageWhenCurrentUserOwnerOfMessage() {
        GroupMessage testMessage = getGroupMessage();
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        when(groupsService.getGroupUser(testGroup, currentUser)).thenReturn(new GroupUser());
        groupsMessagesService.updateMessage(testMessage.getId(), "new message");
        assertNotNull(testMessage.getText());
        assertEquals("new message", testMessage.getText());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/0"),
                any(ResponseMessageUpdatingDTO.class));
    }

    @Test
    public void updateMessageWhenMessageNotFound() {
        when(messagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(MessageException.class, () -> groupsMessagesService.updateMessage(1L, "new message"));
    }

    @Test
    public void updateMessageWhenCurrentUserNotOwnerOfMessage() {
        GroupMessage testMessage = getGroupMessage();
        testMessage.setMessageOwner(testUser);
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        when(groupsService.getGroupUser(testGroup, currentUser)).thenReturn(new GroupUser());
        assertThrows(MessageException.class, () -> groupsMessagesService.updateMessage(testMessage.getId(), "new message"));
        try {
            groupsMessagesService.updateMessage(testMessage.getId(), "new message");
        } catch (MessageException e) {
            assertEquals("User must be owner of this message", e.getMessage());
        }
    }

    @Test
    public void updateMessageWhenContentTypeEqualsImage() {
        GroupMessage testMessage = getGroupMessage();
        testMessage.setContentType(ContentType.IMAGE);
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        when(groupsService.getGroupUser(testGroup, currentUser)).thenReturn(new GroupUser());
        assertThrows(MessageException.class, () -> groupsMessagesService.updateMessage(testMessage.getId(), "new message"));
        try {
            groupsMessagesService.updateMessage(testMessage.getId(), "new message");
        } catch (MessageException e) {
            assertEquals("The file cannot be changed", e.getMessage());
        }
    }

    private GroupMessage getGroupMessage() {
        GroupMessage testMessage = new GroupMessage();
        testMessage.setId(1);
        testMessage.setText("test message");
        testMessage.setMessageOwner(currentUser);
        testMessage.setContentType(ContentType.TEXT);
        testMessage.setGroup(testGroup);
        return testMessage;
    }
}