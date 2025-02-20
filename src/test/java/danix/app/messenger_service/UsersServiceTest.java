package danix.app.messenger_service;


import danix.app.messenger_service.dto.ResponseAppMessageDTO;
import danix.app.messenger_service.dto.ResponseUpdateUserOnlineStatusDTO;
import danix.app.messenger_service.dto.ResponseUserDTO;
import danix.app.messenger_service.dto.ShowUserDTO;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.TokensService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.UserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import util.TestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static util.TestUtils.webSocketUUID;

@ExtendWith(MockitoExtension.class)
public class UsersServiceTest {

    private final User currentUser = TestUtils.getTestCurrentUser();

    private final User user = TestUtils.getTestUser();

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private BlockedUsersRepository blockedUsersRepository;

    @Mock
    private TokensService tokensService;

    @Mock
    private UsersFriendsRepository usersFriendsRepository;

    @Mock
    private BannedUsersRepository bannedUsersRepository;

    @Mock
    private AppMessagesRepository appMessagesRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ChatsRepository chatsRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private UserService userService;

    @Test
    public void findUserWhenUserFoundAndNotEqualToCurrentUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersRepository.findByUsername("Username")).thenReturn(Optional.of(user));
        when(modelMapper.map(user, ShowUserDTO.class)).thenReturn(getShowUserDTO());
        ShowUserDTO userDTO = userService.findUser("Username");
        Assertions.assertNotNull(userDTO);
        assertEquals(userDTO.getUsername(), user.getUsername());
    }

    @Test
    public void findUserWhenUserIdEqualsToCurrentUserId() {
        user.setId(currentUser.getId());
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersRepository.findByUsername("Username")).thenReturn(Optional.of(user));
        ShowUserDTO userDTO = userService.findUser("Username");
        assertNull(userDTO);
    }

    @Test
    public void findUserWhenUserNotFound() {
        when(usersRepository.findByUsername("Username")).thenReturn(Optional.empty());
        ShowUserDTO userDTO = userService.findUser("Username");
        assertNull(userDTO);
    }

    @Test
    public void banUserWhenUserFoundAndUserNotBanned() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        userService.banUser(user.getId(), "Test");
        assertEquals(User.Status.BANNED, user.getUserStatus());
        assertTrue(user.isBanned());
        verify(tokensService, times(1)).banUserTokens(user.getId());
        verify(bannedUsersRepository, times(1)).save(any(BannedUser.class));
        verify(kafkaTemplate, times(1)).send(eq("ban_user-topic"), any(), any());
    }

    @Test
    public void banUserWhenUserNotFound() {
        when(usersRepository.findById(100)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.banUser(100, "Test"));
    }

    @Test
    public void banUserWhenUserIsAlreadyBanned() {
        user.setUserStatus(User.Status.BANNED);
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        assertThrows(UserException.class, () -> userService.banUser(user.getId(), "Test"));
    }

    @Test
    public void unbanUserWhenUserFoundAndIsBanned() {
        user.setUserStatus(User.Status.BANNED);
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(bannedUsersRepository.findByUser(user)).thenReturn(Optional.of(new BannedUser("Test", user)));
        userService.unbanUser(user.getId());
        assertEquals(User.Status.REGISTERED, user.getUserStatus());
        assertFalse(user.isBanned());
        verify(bannedUsersRepository, times(1)).delete(any(BannedUser.class));
        verify(kafkaTemplate, times(1)).send(eq("unban_user-topic"), any());
    }

    @Test
    public void unbanUserNotFound() {
        when(usersRepository.findById(100)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.unbanUser(100));
    }

    @Test
    public void unbanUserWhenUserNotBanned() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(bannedUsersRepository.findByUser(user)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.unbanUser(user.getId()));
    }

    @Test
    public void registerUserWhenTempUserFound() {
        user.setUserStatus(User.Status.TEMPORALLY_REGISTERED);
        when(usersRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        userService.registerUser(user.getEmail());
        assertEquals(User.Status.REGISTERED, user.getUserStatus());
        verify(kafkaTemplate, times(1)).send(eq("registration-topic"), any(), any());
    }

    @Test
    public void registerUserWhenUserNotFound() {
        when(usersRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.registerUser("test@gmail.com"));
    }

    @Test
    public void registerUserWhenUserIsAlreadyRegistered() {
        when(usersRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        assertThrows(UserException.class, () -> userService.registerUser("test@gmail.com"));
    }

    @Test
    public void addFriendWhenUserFoundAndCurrentUserNotBlockedByUserAndFriendNotExistAndUserBlockedByCurrentUser() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)).thenReturn(Optional.of(new BlockedUser()));
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.empty());
        userService.addFriend(user.getId());
        verify(usersFriendsRepository, times(1)).save(any(UserFriend.class));
        verify(blockedUsersRepository, times(1)).delete(any(BlockedUser.class));
    }

    @Test
    public void addFriendWhenUserNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.addFriend(user.getId()));
    }

    @Test
    public void addFriendWhenCurrentUserIsBlockedByUser() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(UserException.class, () -> userService.addFriend(user.getId()));
    }

    @Test
    public void addFriendWhenFriendAlreadyExist() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.of(new UserFriend()));
        assertThrows(UserException.class, () -> userService.addFriend(user.getId()));
    }

    @Test
    public void acceptFriendRequestWhenRequestFoundAndNotAccepted() {
        UserFriend userFriend = new UserFriend();
        userFriend.setStatus(UserFriend.FriendsStatus.WAITING);
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.of(userFriend));
        userService.acceptFriend(user.getId());
        assertEquals(UserFriend.FriendsStatus.ACCEPTED, userFriend.getStatus());
    }

    @Test
    public void acceptFriendRequestWhenUserNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.acceptFriend(user.getId()));
    }

    @Test
    public void acceptFriendRequestWhenRequestNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.acceptFriend(user.getId()));
    }

    @Test
    public void acceptFriendRequestWhenRequestIsAccepted() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        UserFriend userFriend = new UserFriend();
        userFriend.setStatus(UserFriend.FriendsStatus.ACCEPTED);
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.of(userFriend));
        assertThrows(UserException.class, () -> userService.acceptFriend(user.getId()));
    }

    @Test
    public void cancelFriendRequestWhenRequestFoundAndNotAccepted() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        UserFriend userFriend = new UserFriend();
        userFriend.setStatus(UserFriend.FriendsStatus.WAITING);
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.of(userFriend));
        userService.cancelFriendRequest(user.getId());
        verify(usersFriendsRepository, times(1)).delete(any(UserFriend.class));
    }

    @Test
    public void cancelFriendRequestWhenUserNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.cancelFriendRequest(user.getId()));
    }

    @Test
    public void cancelFriendRequestWhenRequestNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.cancelFriendRequest(user.getId()));
    }

    @Test
    public void cancelFriendRequestWhenRequestIsAccepted() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        UserFriend userFriend = new UserFriend();
        userFriend.setStatus(UserFriend.FriendsStatus.ACCEPTED);
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.of(userFriend));
        assertThrows(UserException.class, () -> userService.cancelFriendRequest(user.getId()));
    }

    @Test
    public void deleteFriendWhenFriendFoundByCurrentUser() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.of(new UserFriend()));
        userService.deleteFriend(user.getId());
        verify(usersFriendsRepository, times(1)).delete(any(UserFriend.class));
    }

    @Test
    public void deleteFriendWhenFriendFoundByUserAndNotFoundByCurrentUser() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.of(new UserFriend()));
        userService.deleteFriend(user.getId());
        verify(usersFriendsRepository, times(1)).delete(any(UserFriend.class));
    }

    @Test
    public void deleteFriendWhenUserNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.deleteFriend(user.getId()));
    }

    @Test
    public void deleteFriendWhenFriendNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.deleteFriend(user.getId()));
    }

    @Test
    public void blockUserWhenUserFoundAndUserNotBlocked() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.empty());
        userService.blockUser(user.getId());
        verify(blockedUsersRepository, times(1)).save(any(BlockedUser.class));
    }

    @Test
    public void blockUserWhenUserIsFriendByCurrentUser() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.of(new UserFriend()));
        userService.blockUser(user.getId());
        verify(usersFriendsRepository, times(1)).delete(any(UserFriend.class));
        verify(blockedUsersRepository, times(1)).save(any(BlockedUser.class));
    }

    @Test
    public void blockUserWhenUserIsFriendByUser() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(currentUser, user)).thenReturn(Optional.empty());
        when(usersFriendsRepository.findByOwnerAndFriend(user, currentUser)).thenReturn(Optional.of(new UserFriend()));
        userService.blockUser(user.getId());
        verify(usersFriendsRepository, times(1)).delete(any(UserFriend.class));
        verify(blockedUsersRepository, times(1)).save(any(BlockedUser.class));
    }

    @Test
    public void blockUserWhenUserNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.blockUser(user.getId()));
    }

    @Test
    public void blockUserWhenUserIsBlocked() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(UserException.class, () -> userService.blockUser(user.getId()));
    }

    @Test
    public void unblockUserWhenUserFoundAndUserBlocked() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)).thenReturn(Optional.of(new BlockedUser()));
        userService.unblockUser(user.getId());
        verify(blockedUsersRepository, times(1)).delete(any(BlockedUser.class));
    }

    @Test
    public void unblockUserWhenUserNotFound() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.unblockUser(user.getId()));
    }

    @Test
    public void unblockUserWhenUserIsNotBlocked() {
        when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.unblockUser(user.getId()));
    }

    @Test
    public void updateOnlineStatusToOffline() {
        currentUser.setChannels(Collections.emptyList());
        currentUser.setGroups(Collections.emptyList());
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(chatsRepository.findByUser1OrUser2(currentUser, currentUser)).thenReturn(Collections.emptyList());
        userService.updateOnlineStatus();
        assertEquals(User.OnlineStatus.OFFLINE, currentUser.getOnlineStatus());
    }

    @Test
    public void updateOnlineStatusToOnlineAndSendMessagesToTopics() {
        User testChatUser1 = new User();
        testChatUser1.setWebSocketUUID(webSocketUUID());
        User testChatUser2 = new User();
        testChatUser2.setWebSocketUUID(webSocketUUID());
        Channel testChannel1 = Channel.builder().webSocketUUID(webSocketUUID()).build();
        Channel testChannel2 = Channel.builder().webSocketUUID(webSocketUUID()).build();
        Channel testChannel3 = Channel.builder().webSocketUUID(webSocketUUID()).build();
        currentUser.setChannels(List.of(
                new ChannelUser(currentUser, testChannel1),
                new ChannelUser(currentUser, testChannel2),
                new ChannelUser(currentUser, testChannel3)
        ));
        Group testGroup1 = Group.builder().webSocketUUID(webSocketUUID()).build();
        Group testGroup2 = Group.builder().webSocketUUID(webSocketUUID()).build();
        Group testGroup3 = Group.builder().webSocketUUID(webSocketUUID()).build();
        currentUser.setGroups(List.of(
                new GroupUser(currentUser, testGroup1, false),
                new GroupUser(currentUser, testGroup2, false),
                new GroupUser(currentUser, testGroup3, false)
        ));
        currentUser.setOnlineStatus(User.OnlineStatus.OFFLINE);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        List<Chat> chats = List.of(
                new Chat(testChatUser1, currentUser, UUID.randomUUID().toString()),
                new Chat(testChatUser2, currentUser, UUID.randomUUID().toString()),
                new Chat(currentUser, user, UUID.randomUUID().toString())
        );
        when(chatsRepository.findByUser1OrUser2(currentUser, currentUser)).thenReturn(chats);
        userService.updateOnlineStatus();
        assertEquals(User.OnlineStatus.ONLINE, currentUser.getOnlineStatus());
        chats.forEach(chat -> verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/" + chat.getWebSocketUUID()),
                any(ResponseUpdateUserOnlineStatusDTO.class)));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + testChatUser1.getWebSocketUUID() + "/main"),
                any(ResponseUpdateUserOnlineStatusDTO.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + testChatUser2.getWebSocketUUID() + "/main"),
                any(ResponseUpdateUserOnlineStatusDTO.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + user.getWebSocketUUID() + "/main"),
                any(ResponseUpdateUserOnlineStatusDTO.class));
        currentUser.getChannels().forEach(channelUser -> verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" +
                    channelUser.getChannel().getWebSocketUUID()), any(ResponseUpdateUserOnlineStatusDTO.class)));
        currentUser.getGroups().forEach(groupUser -> verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group/" +
                    groupUser.getGroup().getWebSocketUUID()), any(ResponseUpdateUserOnlineStatusDTO.class)));
    }

    @Test
    public void getFriendRequests() {
        List<UserFriend> testFriendRequests = List.of(
                new UserFriend(user, currentUser, UserFriend.FriendsStatus.WAITING),
                new UserFriend(user, currentUser, UserFriend.FriendsStatus.WAITING),
                new UserFriend(user, currentUser, UserFriend.FriendsStatus.WAITING),
                new UserFriend(user, currentUser, UserFriend.FriendsStatus.ACCEPTED),
                new UserFriend(user, currentUser, UserFriend.FriendsStatus.ACCEPTED)
        );
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByFriend(currentUser)).thenReturn(testFriendRequests);
        List<ResponseUserDTO> testResponseFriendRequests = userService.getAllFriendRequests();
        assertNotNull(testResponseFriendRequests);
        assertNotEquals(testResponseFriendRequests.size(), testFriendRequests.size());
        assertEquals(3, testResponseFriendRequests.size());
    }

    @Test
    public void getFriends() {
        List<UserFriend> testFriends = List.of(
                new UserFriend(user, currentUser, UserFriend.FriendsStatus.WAITING),
                new UserFriend(currentUser, user, UserFriend.FriendsStatus.WAITING),
                new UserFriend(user, currentUser, UserFriend.FriendsStatus.ACCEPTED),
                new UserFriend(currentUser, user, UserFriend.FriendsStatus.ACCEPTED)
        );
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(usersFriendsRepository.findByOwnerOrFriend(currentUser, currentUser)).thenReturn(testFriends);
        List<ShowUserDTO> testResponseFriends = userService.getAllUserFriends();
        assertNotNull(testResponseFriends);
        assertNotEquals(testResponseFriends.size(), testFriends.size());
        assertEquals(2, testResponseFriends.size());
    }

    @Test
    public void getAppMessages() {
        List<AppMessage> testAppMessages = List.of(
                new AppMessage("Test message", currentUser),
                new AppMessage("Test message", currentUser),
                new AppMessage("Test message", currentUser),
                new AppMessage("Test message", currentUser)
        );
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
        when(appMessagesRepository.findByUser(currentUser)).thenReturn(testAppMessages);
        List<ResponseAppMessageDTO> testResponseAppMessages = userService.getAppMessages();
        assertNotNull(testResponseAppMessages);
        assertEquals(testResponseAppMessages.size(), testAppMessages.size());
    }

    private ShowUserDTO getShowUserDTO() {
        ShowUserDTO showUserDTO = new ShowUserDTO();
        showUserDTO.setUsername(user.getUsername());
        showUserDTO.setDescription(user.getDescription());
        showUserDTO.setOnlineStatus(user.getOnlineStatus());
        showUserDTO.setBanned(user.isBanned());
        showUserDTO.setId(user.getId());
        return showUserDTO;
    }
}