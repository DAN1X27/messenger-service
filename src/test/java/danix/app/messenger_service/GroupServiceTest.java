package danix.app.messenger_service;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.GroupsService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.GroupException;
import org.apache.zookeeper.Op;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import util.TestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static util.TestUtils.webSocketUUID;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    private final User currentUser = TestUtils.getTestCurrentUser();

    private final User testUser = TestUtils.getTestUser();

    private final Group testGroup = TestUtils.getTestGroup();

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GroupsActionsMessagesRepository groupsActionsMessagesRepository;

    @Mock
    private AppMessagesRepository appMessagesRepository;

    @Mock
    private GroupsInvitesRepository groupsInvitesRepository;

    @Mock
    private BlockedUsersRepository blockedUsersRepository;

    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private GroupsUsersRepository groupsUsersRepository;

    @Mock
    private GroupsMessagesRepository messagesRepository;

    @Mock
    private GroupsRepository groupsRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GroupsService groupsService;

    @BeforeEach
    public void setUp() {
        testGroup.setBannedUsers(new ArrayList<>());
    }

    @Test
    public void invite() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        when(groupsInvitesRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        groupsService.inviteUser(testGroup.getId(), testUser.getId());
        verify(groupsInvitesRepository, times(1)).save(any(GroupInvite.class));
    }

    @Test
    public void inviteWhenSenderBlockedFromUser() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteWhenUserBlockedByCurrentUser() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteWhenGroupNotFound() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.empty());
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteWhenSenderNotExistsInGroup() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.empty());
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteUserWhenUserExistsInGroup() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(new GroupUser()));
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteUserWhenUserBannedFromGroup() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        testGroup.getBannedUsers().add(testUser);
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteUserWhenUserAlreadyHasInvite() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        when(groupsInvitesRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(new GroupInvite()));
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteUserWhenUserIsPrivate() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        testUser.setIsPrivate(true);
        assertThrows(GroupException.class, () -> groupsService.inviteUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void inviteUserWhenUserIsPrivateAndUserIsFriend() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        testUser.setIsPrivate(true);
        when(userService.findUserFriend(testUser, currentUser)).thenReturn(new UserFriend());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(testUser, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        when(groupsInvitesRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        groupsService.inviteUser(testGroup.getId(), testUser.getId());
        verify(groupsInvitesRepository, times(1)).save(any(GroupInvite.class));
    }

    @Test
    public void acceptInvite() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(currentUser.getId())).thenReturn(currentUser);
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsInvitesRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupInvite()));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.empty());
        groupsService.acceptInviteToGroup(testGroup.getId());
        verify(groupsUsersRepository, times(1)).save(any(GroupUser.class));
        verify(groupsInvitesRepository, times(1)).delete(any(GroupInvite.class));
        verify(groupsActionsMessagesRepository, times(1)).save(any(GroupActionMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/" + testGroup.getWebSocketUUID()),
                any(ResponseGroupActionMessageDTO.class));
    }

    @Test
    public void acceptInviteWhenInviteNotFound() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(currentUser.getId())).thenReturn(currentUser);
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsInvitesRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.empty());
        assertThrows(GroupException.class, () -> groupsService.acceptInviteToGroup(testGroup.getId()));
    }

    @Test
    public void acceptInviteWhenUserAlreadyInGroup() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(currentUser.getId())).thenReturn(currentUser);
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsInvitesRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupInvite()));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        assertThrows(GroupException.class, () -> groupsService.acceptInviteToGroup(testGroup.getId()));
    }

    @Test
    public void acceptInviteWhenUserBannedFromGroup() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(userService.getById(currentUser.getId())).thenReturn(currentUser);
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsInvitesRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupInvite()));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.empty());
        testGroup.getBannedUsers().add(currentUser);
        assertThrows(GroupException.class, () -> groupsService.acceptInviteToGroup(testGroup.getId()));
    }

    @Test
    public void addAdmin() {
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        testGroup.setOwner(currentUser);
        GroupUser groupUser = new GroupUser();
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(groupUser));
        groupsService.addAdmin(testGroup.getId(), testUser.getId());
        assertTrue(groupUser.isAdmin());
    }

    @Test
    public void addAdminWhenCurrentUserNotOwnerOfGroup() {
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        User testOwner = new User();
        testOwner.setId(100);
        testGroup.setOwner(testOwner);
        assertThrows(GroupException.class, () -> groupsService.addAdmin(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void addAdminWhenUserNotExistsInGroup() {
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        testGroup.setOwner(currentUser);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        assertThrows(GroupException.class, () -> groupsService.addAdmin(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void addAdminWhenUserIsAlreadyAdmin() {
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        testGroup.setOwner(currentUser);
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(groupUser));
        assertThrows(GroupException.class, () -> groupsService.addAdmin(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void deleteAdmin() {
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        testGroup.setOwner(currentUser);
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(groupUser));
        groupsService.deleteAdmin(testGroup.getId(), testUser.getId());
        assertFalse(groupUser.isAdmin());
    }

    @Test
    public void deleteAdminWhenUserIsNotAdmin() {
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        testGroup.setOwner(currentUser);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(new GroupUser()));
        assertThrows(GroupException.class, () -> groupsService.deleteAdmin(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void leaveGroupWhenUserNotOwner() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        User testOwner = new User();
        testOwner.setId(100);
        testGroup.setOwner(testOwner);
        groupsService.leaveGroup(testGroup.getId());
        verify(groupsUsersRepository, times(1)).delete(any(GroupUser.class));
        verify(groupsActionsMessagesRepository, times(1)).save(any(GroupActionMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/" + testGroup.getWebSocketUUID()),
                any(ResponseGroupActionMessageDTO.class));
    }

    @Test
    public void leaveGroupWhenUserIsOwner() {
        testGroup.setOwner(currentUser);
        User testUser1 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser2 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser3 = User.builder().webSocketUUID(webSocketUUID()).build();
        testGroup.setUsers(List.of(
                new GroupUser(testUser1, testGroup, false),
                new GroupUser(testUser2, testGroup, false),
                new GroupUser(testUser3, testGroup, false)
        ));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        when(messagesRepository.findAllByGroupAndContentTypeIsNot(eq(testGroup), eq(ContentType.TEXT), any()))
                .thenReturn(Collections.emptyList());
        CompletableFuture<Void> future = groupsService.leaveGroup(testGroup.getId());
        future.join();
        verify(groupsRepository).deleteById(testGroup.getId());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/" + testGroup.getWebSocketUUID()),
                any(ResponseDeletionGroupDTO.class));
    }

    @Test
    public void banUserWhenUserNotExistsInGroup() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(groupUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.empty());
        groupsService.banUser(testGroup.getId(), testUser.getId());
        assertTrue(testGroup.getBannedUsers().contains(testUser));
    }

    @Test
    public void banUserWhenUserExistsInGroupAndNotAdminAndSendMessagesToTopics() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(groupUser));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(new GroupUser()));
        groupsService.banUser(testGroup.getId(), testUser.getId());
        verify(groupsUsersRepository, times(1)).delete(any(GroupUser.class));
        verify(appMessagesRepository, times(1)).save(any(AppMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/"
              + testUser.getWebSocketUUID() + "/main"), any(ResponseAppMessageDTO.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/"
                + testUser.getWebSocketUUID() + "/main"), any(ResponseDeletionGroupDTO.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/" + testGroup.getWebSocketUUID()),
                any(Map.class));
        assertTrue(testGroup.getBannedUsers().contains(testUser));
    }

    @Test
    public void banUserWhenUserIsAdminAndCurrentUserNotOwner() {
        User testOwner = new User();
        testOwner.setId(100);
        testGroup.setOwner(testOwner);
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(groupUser));
        GroupUser foundGroupUser = new GroupUser();
        foundGroupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(foundGroupUser));
        assertThrows(GroupException.class, () -> groupsService.banUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void banUserWhenUserIsAdminAndCurrentUserIsOwner() {
        testGroup.setOwner(currentUser);
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(groupUser));
        GroupUser foundGroupUser = new GroupUser();
        foundGroupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(Optional.of(foundGroupUser));
        groupsService.banUser(testGroup.getId(), testUser.getId());
        assertTrue(testGroup.getBannedUsers().contains(testUser));
    }

    @Test
    public void banUserWhenCurrentUserIsNotAdmin() {
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        assertThrows(GroupException.class, () -> groupsService.banUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void unbanUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(groupUser));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        testGroup.getBannedUsers().add(testUser);
        groupsService.unbanUser(testGroup.getId(), testUser.getId());
        assertTrue(testGroup.getBannedUsers().isEmpty());
    }

    @Test
    public void unbanUserWhenCurrentUserIsNotAdmin() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(new GroupUser()));
        assertThrows(GroupException.class, () -> groupsService.unbanUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void unbanUserWhenUserIsNotBanned() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        GroupUser groupUser = new GroupUser();
        groupUser.setAdmin(true);
        when(groupsUsersRepository.findByGroupAndUser(testGroup, currentUser)).thenReturn(Optional.of(groupUser));
        when(userService.getById(testUser.getId())).thenReturn(testUser);
        assertThrows(GroupException.class, () -> groupsService.unbanUser(testGroup.getId(), testUser.getId()));
    }

    @Test
    public void deleteGroup() {
        testGroup.setOwner(currentUser);
        User testUser1 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser2 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser3 = User.builder().webSocketUUID(webSocketUUID()).build();
        testGroup.setUsers(List.of(
                new GroupUser(testUser1, testGroup, false),
                new GroupUser(testUser2, testGroup, false),
                new GroupUser(testUser3, testGroup, false)
        ));
        testGroup.setMessages(Collections.emptyList());
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(messagesRepository.findAllByGroupAndContentTypeIsNot(eq(testGroup), eq(ContentType.TEXT), any()))
                .thenReturn(Collections.emptyList());
        CompletableFuture<Void> future = groupsService.deleteGroup(testGroup.getId());
        future.join();
        verify(groupsRepository).deleteById(testGroup.getId());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/" + testGroup.getWebSocketUUID()),
                any(ResponseDeletionGroupDTO.class));
    }

    @Test
    public void deleteGroupWhenCurrentUserNotOwner() {
        User testOwner = new User();
        testOwner.setId(3);
        testGroup.setOwner(testOwner);
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        assertThrows(GroupException.class, () -> groupsService.deleteGroup(testGroup.getId()));
    }

    @Test
    public void updateGroup() {
        UpdateGroupDTO updateGroupDTO = new UpdateGroupDTO();
        updateGroupDTO.setName("New name");
        updateGroupDTO.setGroupId(testGroup.getId());
        testGroup.setOwner(currentUser);
        User testUser1 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser2 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser3 = User.builder().webSocketUUID(webSocketUUID()).build();
        testGroup.setUsers(List.of(
                new GroupUser(testUser2, testGroup, false),
                new GroupUser(testUser1, testGroup, false),
                new GroupUser(testUser3, testGroup, false)
        ));
        when(groupsRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        groupsService.updateGroup(updateGroupDTO);
        assertEquals(updateGroupDTO.getName(), testGroup.getName());
        assertNotNull(testGroup.getDescription());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/" + testGroup.getWebSocketUUID()),
                any(ResponseGroupUpdatingDTO.class));
    }

    @Test
    public void getUserGroups() {
        Group testGroup1 = new Group();
        testGroup1.setName("TestGroup1");
        Group testGroup2 = new Group();
        testGroup2.setName("TestGroup2");
        GroupUser testGroupUser1 = new GroupUser();
        testGroupUser1.setGroup(testGroup1);
        GroupUser testGroupUser2 = new GroupUser();
        testGroupUser2.setGroup(testGroup2);
        List<GroupUser> testGroups = List.of(testGroupUser1, testGroupUser2);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(groupsUsersRepository.findAllByUser(currentUser)).thenReturn(testGroups);
        when(modelMapper.map(testGroup1, ResponseGroupDTO.class)).thenReturn(new ResponseGroupDTO(testGroup1.getName()));
        when(modelMapper.map(testGroup2, ResponseGroupDTO.class)).thenReturn(new ResponseGroupDTO(testGroup2.getName()));
        List<ResponseGroupDTO> responseGroups = groupsService.getAllUserGroups();
        assertEquals(responseGroups.get(0).getName(), testGroup1.getName());
        assertEquals(responseGroups.get(1).getName(), testGroup2.getName());
    }
}