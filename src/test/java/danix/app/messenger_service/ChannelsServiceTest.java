package danix.app.messenger_service;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.ChannelsService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.ChannelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import util.TestUtils;

import java.util.*;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static util.TestUtils.webSocketUUID;

@ExtendWith(MockitoExtension.class)
public class ChannelsServiceTest {

    private final User currenusUser = TestUtils.getTestCurrentUser();

    private final User user = TestUtils.getTestUser();

    private final Channel testChannel = TestUtils.getTestChannel();

    @Mock
    private ChannelsPostsRepository channelsPostsRepository;

    @Mock
    private ChannelsPostsFilesRepository postsImagesRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ChannelsPostsCommentsRepository postCommentsRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private BlockedUsersRepository blockedUsersRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private AppMessagesRepository appMessagesRepository;

    @Mock
    private ChannelsLogsRepository channelsLogsRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ChannelsUsersRepository channelsUsersRepository;

    @Mock
    private ChannelsRepository channelsRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ChannelsInvitesRepository channelsInvitesRepository;

    @InjectMocks
    private ChannelsService channelsService;

    @BeforeEach
    public void setUp() {
        testChannel.setBannedUsers(new ArrayList<>());
    }

    @Test
    public void createWhenNameIsBusy() {
        CreateChannelDTO createChannelDTO = new CreateChannelDTO();
        createChannelDTO.setName("test_name");
        when(channelsRepository.findByName(createChannelDTO.getName())).thenReturn(Optional.of(new Channel()));
        assertThrows(ChannelException.class, () -> channelsService.createChannel(createChannelDTO));
    }

    @Test
    public void createChannelWhenNameIsNotBusy() {
        CreateChannelDTO createChannelDTO = new CreateChannelDTO();
        createChannelDTO.setName("test_name");
        createChannelDTO.setIsPrivate(false);
        when(channelsRepository.findByName(createChannelDTO.getName())).thenReturn(Optional.empty());
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        channelsService.createChannel(createChannelDTO);
        verify(channelsRepository, times(1)).save(any());
        verify(channelsUsersRepository, times(1)).save(any());
    }

    @Test
    public void findChannelWhenChannelFoundAndNotPrivateAndNotBannedAndUserNotAdmin() {
        when(channelsRepository.findByNameStartsWith(testChannel.getName())).thenReturn(Optional.of(testChannel));
        when(modelMapper.map(testChannel, ResponseChannelDTO.class)).thenReturn(getResponseChannelDTO());
        ResponseChannelDTO responseChannelDTO = channelsService.findChannel(testChannel.getName());
        assertNotNull(responseChannelDTO);
        assertEquals(responseChannelDTO.getName(), testChannel.getName());
    }

    @Test
    public void findChannelWhenChannelNotFound() {
        when(channelsRepository.findByNameStartsWith(testChannel.getName())).thenReturn(Optional.empty());
        assertNull(channelsService.findChannel(testChannel.getName()));
    }

    @Test
    public void findChannelWhenChannelFoundAndIsPrivate() {
        testChannel.setPrivate(true);
        when(channelsRepository.findByNameStartsWith(testChannel.getName())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        assertNull(channelsService.findChannel(testChannel.getName()));
    }

    @Test
    public void findChannelWhenChannelFoundAndIsBanned() {
        testChannel.setBaned(true);
        when(channelsRepository.findByNameStartsWith(testChannel.getName())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        assertNull(channelsService.findChannel(testChannel.getName()));
        testChannel.setBaned(false);
    }

    @Test
    public void findChannelWhenChannelFoundAndPrivateAndBannedAndCurrentUserIsAdmin() {
        testChannel.setPrivate(true);
        testChannel.setBaned(true);
        currenusUser.setRole(User.Roles.ROLE_ADMIN);
        when(channelsRepository.findByNameStartsWith(testChannel.getName())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(modelMapper.map(testChannel, ResponseChannelDTO.class)).thenReturn(getResponseChannelDTO());
        ResponseChannelDTO responseChannelDTO = channelsService.findChannel(testChannel.getName());
        assertNotNull(responseChannelDTO);
        assertEquals(responseChannelDTO.getName(), testChannel.getName());
        currenusUser.setRole(User.Roles.ROLE_USER);
        testChannel.setBaned(false);
        testChannel.setPrivate(false);
    }

    @Test
    public void inviteToChannelWhenUserFoundAndChannelFound() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(channelsInvitesRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        channelsService.inviteToChannel(testChannel.getId(), user.getId());
        verify(channelsInvitesRepository, times(1)).save(any(ChannelInvite.class));
    }

    @Test
    public void inviteChannelWheChannelNotFound() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void inviteToChannelWhenInvitesNotAllowed() {
        testChannel.setInvitesAllowed(false);
        User testOwner = new User();
        testOwner.setId(3);
        testChannel.setOwner(testOwner);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void inviteToChannelWhenCurrentUserBlockedFromUser() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void inviteWhenUserBlockedByCurrentUser() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndBlockedUser(currenusUser, user)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void inviteToChannelWhenInvitesNotAllowedAndCurrentUserChannelOwner() {
        testChannel.setInvitesAllowed(false);
        testChannel.setOwner(currenusUser);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(channelsInvitesRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        channelsService.inviteToChannel(testChannel.getId(), user.getId());
        verify(channelsInvitesRepository, times(1)).save(any(ChannelInvite.class));
    }

    @Test
    public void inviteToChannelWhenUserPrivate() {
        user.setIsPrivate(true);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(userService.findUserFriend(user, currenusUser)).thenReturn(null);
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void inviteToChannelWhenUserPrivateAndUserIsFriend() {
        user.setIsPrivate(true);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(userService.findUserFriend(user, currenusUser)).thenReturn(new UserFriend());
        when(channelsInvitesRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        channelsService.inviteToChannel(testChannel.getId(), user.getId());
        verify(channelsInvitesRepository, times(1)).save(any(ChannelInvite.class));
    }

    @Test
    public void inviteToChannelWhenUserAlreadyInvited() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(channelsInvitesRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(new ChannelInvite()));
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void inviteToChannelWhenUserAlreadyExistsInChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(channelsInvitesRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void inviteToChannelWhenUserBannedFromChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        when(blockedUsersRepository.findByOwnerAndBlockedUser(user, currenusUser)).thenReturn(Optional.empty());
        when(channelsInvitesRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        testChannel.getBannedUsers().add(user);
        assertThrows(ChannelException.class, () -> channelsService.inviteToChannel(testChannel.getId(), user.getId()));
    }

    @Test
    public void acceptInviteWhenChannelFoundAndInviteFoundAndUserNotExistInChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsInvitesRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelInvite()));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.empty());
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        channelsService.acceptInviteToChannel(testChannel.getId());
        verify(channelsUsersRepository, times(1)).save(any(ChannelUser.class));
        verify(channelsInvitesRepository, times(1)).delete(any(ChannelInvite.class));
    }

    @Test
    public void acceptInviteWhenInviteNotFound() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsInvitesRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.empty());
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        assertThrows(ChannelException.class, () -> channelsService.acceptInviteToChannel(testChannel.getId()));
    }

    @Test
    public void acceptInviteWhenCurrenUserExistsInChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsInvitesRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelInvite()));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        assertThrows(ChannelException.class, () -> channelsService.acceptInviteToChannel(testChannel.getId()));
    }

    @Test
    public void acceptInviteWhenCurrenUserBannedFromChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        when(channelsInvitesRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelInvite()));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.empty());
        testChannel.getBannedUsers().add(currenusUser);
        assertThrows(ChannelException.class, () -> channelsService.acceptInviteToChannel(testChannel.getId()));
    }

    @Test
    public void joinToChannelWhenChannelFoundAndUserNotExistsInChannelAndNotBannedFromChannel() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.empty());
        channelsService.joinToChannel(testChannel.getId());
        verify(channelsUsersRepository, times(1)).save(any(ChannelUser.class));
    }

    @Test
    public void joinToChannelWhenChannelNotFound() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> channelsService.joinToChannel(testChannel.getId()));
    }

    @Test
    public void joinToChannelWhenCurrentUserExistsInChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(new ChannelUser()));
        assertThrows(ChannelException.class, () -> channelsService.joinToChannel(testChannel.getId()));
    }

    @Test
    public void joinToChannelWhenCurrentUserBannedFromChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.empty());
        testChannel.getBannedUsers().add(currenusUser);
        assertThrows(ChannelException.class, () -> channelsService.joinToChannel(testChannel.getId()));
    }

    @Test
    public void joinToChannelWhenChannelIsPrivate() {
        testChannel.setPrivate(true);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(userService.getById(currenusUser.getId())).thenReturn(currenusUser);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> channelsService.joinToChannel(testChannel.getId()));
    }

    @Test
    public void banUserWhenChannelFoundAndCurrentUserAdminOfChannelAndUserFound() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        channelUser.setChannel(testChannel);
        channelUser.setUser(currenusUser);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        channelsService.banUser(testChannel.getId(), user.getId());
        assertTrue(testChannel.getBannedUsers().contains(user));
        verify(channelsLogsRepository, times(1)).save(any(ChannelLog.class));
    }

    @Test
    public void banUserWhenCurrentUserIsNotAdminOfTheChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        assertThrows(ChannelException.class, () -> channelsService.banUser(testChannel.getId(), user.getId()));
    }

    @Test
    public void banUserWhenUserIsBanned() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        testChannel.getBannedUsers().add(user);
        assertThrows(ChannelException.class, () -> channelsService.banUser(testChannel.getId(), user.getId()));
    }

    @Test
    public void banUserWhenUserIsAdminAndCurrenUserNotOwnerOfChannel() {
        User testOwner = new User();
        testOwner.setId(100);
        testChannel.setOwner(testOwner);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(channelUser));
        assertThrows(ChannelException.class, () -> channelsService.banUser(testChannel.getId(), user.getId()));
    }

    @Test
    public void banUserWhenUserIsAdminAndCurrenUserOwnerOfChannel() {
        testChannel.setOwner(currenusUser);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser currentChannelUser = new ChannelUser();
        currentChannelUser.setIsAdmin(true);
        currentChannelUser.setChannel(testChannel);
        currentChannelUser.setUser(currenusUser);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(currentChannelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        channelUser.setChannel(testChannel);
        channelUser.setUser(user);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(channelUser));
        channelsService.banUser(testChannel.getId(), user.getId());
        verify(channelsUsersRepository, times(1)).delete(channelUser);
        verify(appMessagesRepository, times(1)).save(any(AppMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(any(), any(ResponseAppMessageDTO.class));
        verify(messagingTemplate, times(1)).convertAndSend(any(), any(ResponseChannelDeletionDTO.class));
        verify(messagingTemplate, times(1)).convertAndSend(any(), any(Map.class));
        assertTrue(testChannel.getBannedUsers().contains(user));
        verify(channelsLogsRepository, times(1)).save(any(ChannelLog.class));
    }

    @Test
    public void getBannedUsersWhenCurrentUserIsAdminOfTheChannel() {
        testChannel.setBannedUsers(List.of(
                new User(),
                new User(),
                new User()
        ));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        channelUser.setUser(currenusUser);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        List<ResponseUserDTO> bannedUsers = channelsService.getBannedUsers(testChannel.getId());
        assertEquals(testChannel.getBannedUsers().size(), bannedUsers.size());
    }

    @Test
    public void getBannedUsersWhenCurrentUserIsNotAdminOfTheChannel() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        assertThrows(ChannelException.class, () -> channelsService.getBannedUsers(testChannel.getId()));
    }

    @Test
    public void unbanUserWhenCurrentUserIsAdminOfTheChannelAndUserIsBanned() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        testChannel.getBannedUsers().add(user);
        channelsService.unbanUser(testChannel.getId(), user.getId());
        assertTrue(testChannel.getBannedUsers().isEmpty());
        verify(channelsLogsRepository, times(1)).save(any(ChannelLog.class));
    }

    @Test
    public void unbanUserWhenCurrentUserIsNotAdminOfTheChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        assertThrows(ChannelException.class, () -> channelsService.unbanUser(testChannel.getId(), user.getId()));
    }

    @Test
    public void unbanUserWhenUserIsNotBanned() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(channelUser));
        when(userService.getById(user.getId())).thenReturn(user);
        assertThrows(ChannelException.class, () -> channelsService.unbanUser(testChannel.getId(), user.getId()));
    }

    @Test
    public void addAdminWhenCurrentUserIsOwnerAndUserIsNotAdminOfTheChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        testChannel.setOwner(currenusUser);
        when(userService.getById(user.getId())).thenReturn(user);
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(channelUser));
        channelsService.addAdmin(testChannel.getId(), user.getId());
        assertTrue(channelUser.getIsAdmin());
        verify(channelsLogsRepository, times(1)).save(any(ChannelLog.class));
    }

    @Test
    public void addAdminWhenCurrentUserIsNotOwnerOfTheChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        User testOwner = new User();
        testOwner.setId(100);
        testChannel.setOwner(testOwner);
        assertThrows(ChannelException.class, () -> channelsService.addAdmin(testChannel.getId(), user.getId()));
    }

    @Test
    public void addAdminWhenUserNotExistsInChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        testChannel.setOwner(currenusUser);
        when(userService.getById(user.getId())).thenReturn(user);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> channelsService.addAdmin(testChannel.getId(), user.getId()));
    }

    @Test
    public void addAdminWhenUserIsAlreadyAdmin() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        testChannel.setOwner(currenusUser);
        when(userService.getById(user.getId())).thenReturn(user);
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(channelUser));
        assertThrows(ChannelException.class, () -> channelsService.addAdmin(testChannel.getId(), user.getId()));
    }

    @Test
    public void deleteAdminWhenCurrentUserIsOwnerAndUserIsAdminOfTheChannel() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(channelUser));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        testChannel.setOwner(currenusUser);
        channelsService.deleteAdmin(testChannel.getId(), user.getId());
        assertFalse(channelUser.getIsAdmin());
        verify(channelsLogsRepository, times(1)).save(any(ChannelLog.class));
    }

    @Test
    public void deleteAdminWhenCurrentUserIsNotOwnerOfTheChannel() {
        User testOwner = new User();
        testOwner.setId(100);
        testChannel.setOwner(testOwner);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(channelUser));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        assertThrows(ChannelException.class, () -> channelsService.deleteAdmin(testChannel.getId(), user.getId()));
    }

    @Test
    public void deleteAdminWhenUserIsNotAdmin() {
        testChannel.setOwner(currenusUser);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(userService.getById(user.getId())).thenReturn(user);
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        when(channelsUsersRepository.findByUserAndChannel(user, testChannel)).thenReturn(Optional.of(channelUser));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        assertThrows(ChannelException.class, () -> channelsService.deleteAdmin(testChannel.getId(), user.getId()));
    }

    @Test
    public void leaveChannelWhenCurrentUserNotOwner() {
        User testOwner = new User();
        testOwner.setId(100);
        testChannel.setOwner(testOwner);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser testChannelUser = new ChannelUser();
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(testChannelUser));
        channelsService.leaveChannel(testChannel.getId());
        verify(channelsUsersRepository).delete(testChannelUser);
        verify(channelsRepository, never()).delete(testChannel);
    }

    @Test
    public void leaveChannelWhenCurrentUserOwner() {
        testChannel.setOwner(currenusUser);
        testChannel.setUsers(Collections.emptyList());
        testChannel.setPosts(Collections.emptyList());
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        ChannelUser testChannelUser = new ChannelUser();
        when(channelsUsersRepository.findByUserAndChannel(currenusUser, testChannel)).thenReturn(Optional.of(testChannelUser));
        channelsService.leaveChannel(testChannel.getId());
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM channels WHERE id = ?"), eq(testChannel.getId()));
    }

    @Test
    public void banChannelWhenChannelIsNotBanned() {
        testChannel.setOwner(currenusUser);
        User testUser1 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser2 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser3 = User.builder().webSocketUUID(webSocketUUID()).build();
        testChannel.setUsers(List.of(
                new ChannelUser(testUser1, testChannel),
                new ChannelUser(testUser2, testChannel),
                new ChannelUser(testUser3, testChannel)
        ));
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        channelsService.banChannel(testChannel.getId(), "Reason");
        assertTrue(testChannel.isBaned());
        verify(kafkaTemplate, times(1)).send(eq("ban_channel-topic"), any(), any());
        verify(appMessagesRepository, times(1)).save(any(AppMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" +
                currenusUser.getWebSocketUUID() + "/main"), any(ResponseAppMessageDTO.class));
        testChannel.getUsers().forEach(channelUser -> verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + channelUser.getUser().getWebSocketUUID() + "/main"),
                any(ResponseChannelDeletionDTO.class)));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" + testChannel.getWebSocketUUID()),
                any(ResponseChannelDeletionDTO.class));
    }

    @Test
    public void banChannelWhenChannelIsBanned() {
        testChannel.setBaned(true);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        assertThrows(ChannelException.class, () -> channelsService.banChannel(testChannel.getId(), "Reason"));
    }

    @Test
    public void unbanChannelWhenChannelIsBanned() {
        currenusUser.setRole(User.Roles.ROLE_ADMIN);
        testChannel.setOwner(user);
        testChannel.setBaned(true);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        channelsService.unbanChannel(testChannel.getId());
        assertFalse(testChannel.isBaned());
        verify(kafkaTemplate, times(1)).send(eq("unban_channel-topic"), any(), any());
        verify(appMessagesRepository, times(1)).save(any(AppMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + user.getWebSocketUUID() + "/main"),
                any(ResponseAppMessageDTO.class));
    }

    @Test
    public void unbanChannelWhenChanelIsBanned() {
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        assertThrows(ChannelException.class, () -> channelsService.unbanChannel(testChannel.getId()));
    }

    @Test
    public void deleteChannelWhenCurrentUserOwner() {
        testChannel.setOwner(currenusUser);
        User testUser1 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser2 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser3 = User.builder().webSocketUUID(webSocketUUID()).build();
        testChannel.setUsers(List.of(
                new ChannelUser(testUser1, testChannel),
                new ChannelUser(testUser2, testChannel),
                new ChannelUser(testUser3, testChannel)
        ));
        testChannel.setPosts(Collections.emptyList());
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(postCommentsRepository.findAllByPostIn(testChannel.getPosts())).thenReturn(Collections.emptyList());
        channelsService.deleteChannel(testChannel.getId());
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM channels WHERE id = ?"), eq(testChannel.getId()));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" + testChannel.getWebSocketUUID()),
                any(ResponseChannelDeletionDTO.class));
    }

    @Test
    public void deleteChannelWhenCurrentUserNotOwner() {
        testChannel.setOwner(user);
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        assertThrows(ChannelException.class, () -> channelsService.deleteChannel(testChannel.getId()));
    }

    @Test
    public void updateChannelName() {
        UpdateChannelDTO updateChannelDTO = new UpdateChannelDTO();
        updateChannelDTO.setName("updated_name");
        String oldDescription = testChannel.getDescription();
        testChannel.setOwner(currenusUser);
        User testUser1 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser2 = User.builder().webSocketUUID(webSocketUUID()).build();
        User testUser3 = User.builder().webSocketUUID(webSocketUUID()).build();
        testChannel.setUsers(List.of(
                new ChannelUser(testUser1, testChannel),
                new ChannelUser(testUser2, testChannel),
                new ChannelUser(testUser3, testChannel)
        ));
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        when(modelMapper.map(testChannel, ResponseChannelDTO.class)).thenReturn(new ResponseChannelDTO());
        channelsService.updateChannel(updateChannelDTO, testChannel.getId());
        assertEquals(testChannel.getDescription(), oldDescription);
        assertEquals(testChannel.getName(), updateChannelDTO.getName());
        testChannel.getUsers().forEach(channelUser -> verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/user/" + channelUser.getUser().getWebSocketUUID() + "/main"),
                        any(ResponseChannelUpdatingDTO.class)));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" + testChannel.getWebSocketUUID()),
                any(ResponseChannelUpdatingDTO.class));
    }

    @Test
    public void updateChannelDescription() {
        UpdateChannelDTO updateChannelDTO = new UpdateChannelDTO();
        updateChannelDTO.setDescription("updated_description");
        String oldName = testChannel.getName();
        testChannel.setOwner(currenusUser);
        testChannel.setUsers(Collections.emptyList());
        when(channelsRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currenusUser));
        channelsService.updateChannel(updateChannelDTO, testChannel.getId());
        assertEquals(testChannel.getDescription(), updateChannelDTO.getDescription());
        assertEquals(testChannel.getName(), oldName);
    }

    private ResponseChannelDTO getResponseChannelDTO() {
        ResponseChannelDTO responseChannelDTO = new ResponseChannelDTO();
        responseChannelDTO.setId(testChannel.getId());
        responseChannelDTO.setName(testChannel.getName());
        responseChannelDTO.setDescription(testChannel.getDescription());
        return responseChannelDTO;
    }
}