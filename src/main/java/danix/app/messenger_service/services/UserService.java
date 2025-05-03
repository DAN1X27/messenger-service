package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.util.FileException;
import danix.app.messenger_service.util.FilesUtils;
import danix.app.messenger_service.util.UserException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokensService tokensService;
    private final BannedUsersRepository bannedUsersRepository;
    private final UsersFriendsRepository usersFriendsRepository;
    private final BlockedUsersRepository blockedUsersRepository;
    private final AppMessagesRepository appMessagesRepository;
    private final EmailsKeysRepository emailsKeysRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatsRepository chatsRepository;

    @Value("${default_user_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${users_images_path}")
    private String USERS_IMAGES_PATH;

    public ShowUserDTO findUser(String username) {
        User user = usersRepository.findByUsername(username).orElse(null);
        if (user == null || user.getId() == getCurrentUser().getId() || user.getIsPrivate()) {
            return null;
        }
        return modelMapper.map(user, ShowUserDTO.class);
    }

    public UserInfoDTO getUserInfo() {
        return modelMapper.map(getCurrentUser(), UserInfoDTO.class);
    }

    public ResponseFileDTO getImage(int id) {
        User user = getById(id);
        return FilesUtils.download(Path.of(USERS_IMAGES_PATH), user.getImageUUID(), ContentType.IMAGE);
    }

    @Transactional
    public void addImage(MultipartFile imageFile, int id) {
        User currentUser = getById(id);
        String uuid = UUID.randomUUID().toString();
        FilesUtils.upload(Path.of(USERS_IMAGES_PATH), imageFile, uuid, ContentType.IMAGE);
        if (currentUser.getImageUUID().equals(DEFAULT_IMAGE_UUID)) {
            currentUser.setImageUUID(uuid);
            return;
        }
        FilesUtils.delete(Path.of(USERS_IMAGES_PATH), currentUser.getImageUUID());
        currentUser.setImageUUID(uuid);
    }

    @Transactional
    public void deleteImage(int id) {
        User currentUser = getById(id);
        if (currentUser.getImageUUID().equals(DEFAULT_IMAGE_UUID)) {
            throw new FileException("User already have default image");
        }
        FilesUtils.delete(Path.of(USERS_IMAGES_PATH), currentUser.getImageUUID());
        currentUser.setImageUUID(DEFAULT_IMAGE_UUID);
    }

    public User getByEmail(String email) {
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public User getByUsername(String username) {
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public List<ResponseAppMessageDTO> getAppMessages() {
        User currentUser = getCurrentUser();
        return appMessagesRepository.findByUser(currentUser).stream()
                .map(msg -> modelMapper.map(msg, ResponseAppMessageDTO.class))
                .collect(Collectors.toList());
    }

    public List<ResponseUserDTO> getAllFriendRequests() {
        return usersFriendsRepository.findByFriend(getCurrentUser()).stream()
                .filter(userFriend -> userFriend.getStatus() == UserFriend.FriendsStatus.WAITING)
                .map(userFriend -> {
                    ResponseUserDTO userDTO = new ResponseUserDTO();
                    userDTO.setId(userFriend.getOwner().getId());
                    userDTO.setUsername(userFriend.getOwner().getUsername());
                    userDTO.setOnlineStatus(userFriend.getOwner().getOnlineStatus());
                    userDTO.setOnlineStatus(userFriend.getOwner().getOnlineStatus());
                    return userDTO;
                }).collect(Collectors.toList());
    }

    public List<ShowUserDTO> getAllUserFriends() {
        User currentUser = getCurrentUser();
        return usersFriendsRepository.findByOwnerOrFriend(currentUser, currentUser).stream()
                .filter(user -> user.getStatus() == UserFriend.FriendsStatus.ACCEPTED)
                .map(user -> {
                    User friend = user.getFriend().getId() != currentUser.getId() ? user.getFriend()
                            : user.getOwner();
                    return modelMapper.map(friend, ShowUserDTO.class);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEmailKey(EmailKey key) {
        emailsKeysRepository.delete(key);
    }

    @Transactional
    public void deleteTempUser(String email) {
        usersRepository.findByEmail(email).ifPresent(user -> {
            if (user.getUserStatus() == User.Status.TEMPORALLY_REGISTERED) {
                usersRepository.delete(user);
            }
        });
    }

    @Transactional
    public void cancelFriendRequest(int userId) {
        User user = getById(userId);
        deleteFriendRequest(getCurrentUser(), user);
    }

    @Transactional
    public void rejectFriendRequest(int userId) {
        User user = getById(userId);
        deleteFriendRequest(user, getCurrentUser());
    }

    private void deleteFriendRequest(User owner, User friend) {
        usersFriendsRepository.findByOwnerAndFriend(owner, friend).ifPresentOrElse(request -> {
            if (request.getStatus() == UserFriend.FriendsStatus.ACCEPTED) {
                throw new UserException("User is already in your friend list");
            }
            usersFriendsRepository.delete(request);
        }, () -> {
            throw new UserException("Request not found");
        });
    }

    @Transactional
    public void deleteFriend(int id) {
        User friend = getById(id);
        User currentUser = getCurrentUser();
        usersFriendsRepository.findByOwnerAndFriend(currentUser, friend)
                .ifPresentOrElse(usersFriendsRepository::delete, () -> usersFriendsRepository.findByOwnerAndFriend(friend, currentUser)
                        .ifPresentOrElse(usersFriendsRepository::delete, () -> {
                            throw new UserException("User is not exist in your friends");
                        }));
    }

    @Transactional
    public void blockUser(int id) {
        User user = getById(id);
        blockedUsersRepository.findByOwnerAndBlockedUser(getCurrentUser(), user).ifPresentOrElse(blockedUser -> {
            throw new UserException("User is already blocked");
        }, () -> {
            User currentUser = getCurrentUser();
            blockedUsersRepository.save(new BlockedUser(currentUser, user));
            usersFriendsRepository.findByOwnerAndFriend(currentUser, user)
                    .ifPresentOrElse(usersFriendsRepository::delete, () -> usersFriendsRepository.findByOwnerAndFriend(user, currentUser)
                            .ifPresent(usersFriendsRepository::delete));
        });
    }

    @Transactional
    public void unblockUser(int id) {
        User user = getById(id);
        blockedUsersRepository.findByOwnerAndBlockedUser(getCurrentUser(), user)
                .ifPresentOrElse(blockedUsersRepository::delete, () -> {
                    throw new UserException("User is not blocked");
                });
    }

    @Transactional
    public void acceptFriend(int id) {
        User user = getById(id);
        User currentUser = getCurrentUser();
        usersFriendsRepository.findByOwnerAndFriend(user, currentUser).ifPresentOrElse(friendRequest -> {
            if (friendRequest.getStatus() == UserFriend.FriendsStatus.ACCEPTED) {
                throw new UserException("Request is already accepted");
            }
            friendRequest.setStatus(UserFriend.FriendsStatus.ACCEPTED);
        }, () -> {
            throw new UserException("Request not found");
        });
    }

    @Transactional
    public void addFriend(String username) {
        User user = getByUsername(username);
        User currentUser = getCurrentUser();
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresentOrElse(blockedUser -> {
            throw new UserException("The user has blocked you");
        }, () -> blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(blockedUsersRepository::delete));
        UserFriend userFriend = findUserFriend(user, currentUser);
        if (userFriend != null) {
            if (userFriend.getStatus() == UserFriend.FriendsStatus.WAITING) {
                throw new UserException("A request has already been sent to this user");
            }
            throw new UserException("Friend already exists");
        }
        usersFriendsRepository.save(new UserFriend(getCurrentUser(), user, UserFriend.FriendsStatus.WAITING));
    }

    @Transactional
    public void banUser(int id, String reason) {
        User user = getById(id);
        if (user.getUserStatus() == User.Status.BANNED) {
            throw new UserException("User is already banned");
        }
        user.setUserStatus(User.Status.BANNED);
        user.setBanned(true);
        tokensService.banUserTokens(id);
        bannedUsersRepository.save(new BannedUser(reason, user));
        kafkaTemplate.send("ban_user-topic", user.getEmail(), reason);
    }

    @Transactional
    public void unbanUser(int id) {
        User user = getById(id);
        BannedUser bannedUser = bannedUsersRepository.findByUser(user)
                .orElseThrow(() -> new UserException("User is not banned"));
        user.setUserStatus(User.Status.REGISTERED);
        user.setBanned(false);
        bannedUsersRepository.delete(bannedUser);
        kafkaTemplate.send("unban_user-topic", user.getEmail());
    }

    @Transactional
    public void registerUser(String email) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found"));
        if (user.getUserStatus() != User.Status.TEMPORALLY_REGISTERED) {
            throw new UserException("User is already registered");
        }
        user.setUserStatus(User.Status.REGISTERED);
        kafkaTemplate.send("registration-topic", email, user.getUsername());
    }

    @Transactional
    public void update(UpdateUserDTO updateUserDTO) {
        User user = getById(getCurrentUser().getId());
        usersRepository.findByUsername(updateUserDTO.getUsername()).ifPresent(foundUser -> {
            if (foundUser.getId() != user.getId()) {
                throw new UserException("Username is already taken");
            }
        });
        user.setUsername(updateUserDTO.getUsername());
        user.setDescription(updateUserDTO.getDescription());
        user.setIsPrivate(updateUserDTO.isPrivate());
    }

    @Transactional
    public void updatePassword(String password) {
        User user = getById(getCurrentUser().getId());
        user.setPassword(passwordEncoder.encode(password));
    }

    @Transactional
    public void temporalRegister(RegistrationUserDTO personDTO) {
        usersRepository.save(User.builder()
              .username(personDTO.getUsername())
              .createdAt(LocalDateTime.now())
              .description(personDTO.getDescription())
              .email(personDTO.getEmail())
              .role(User.Roles.ROLE_USER)
              .password(passwordEncoder.encode(personDTO.getPassword()))
              .isPrivate(personDTO.getIsPrivate() != null && personDTO.getIsPrivate())
              .imageUUID(DEFAULT_IMAGE_UUID)
              .userStatus(User.Status.TEMPORALLY_REGISTERED)
              .webSocketUUID(UUID.randomUUID().toString())
              .build()
        );
    }

    @Transactional
    public void sendRegistrationKey(String email) {
        Random random = new Random();
        Integer key = random.nextInt(100000, 999999);
        emailsKeysRepository.save(new EmailKey(email, key));
        kafkaTemplate.send("registration_key-topic", email, String.valueOf(key));
    }

    @Transactional
    public void sendRecoverPasswordKey(String email) {
        Random random = new Random();
        Integer key = random.nextInt(100000, 999999);
        emailsKeysRepository.save(new EmailKey(email, key));
        kafkaTemplate.send("recover_password-topic", email, String.valueOf(key));
    }

    @Transactional
    public void updateEmailKeyAttempts(EmailKey key) {
        key.setAttempts(key.getAttempts() + 1);
    }

    @Transactional
    public void setOnlineStatus() {
        User user = getById(getCurrentUser().getId());
        if (user.getOnlineStatus() == User.OnlineStatus.OFFLINE) {
            user.setOnlineStatus(User.OnlineStatus.ONLINE);
            sendOnlineStatusUpdateMessage(user, User.OnlineStatus.ONLINE);
        }
        user.setLastOnlineStatusUpdate(LocalDateTime.now());
    }

    @Transactional
    public void setOfflineStatus() {
        User user = getById(getCurrentUser().getId());
        user.setOnlineStatus(User.OnlineStatus.OFFLINE);
        sendOnlineStatusUpdateMessage(user, User.OnlineStatus.OFFLINE);
    }

    @Transactional
    public void setOfflineStatusForOfflineUsers() {
        List<User> users = usersRepository.findAllByOnlineStatusAndLastOnlineStatusUpdateBefore(User.OnlineStatus.ONLINE,
                LocalDateTime.now().minusMinutes(2));
        usersRepository.updateOnlineStatus(users);
        users.forEach(user -> sendOnlineStatusUpdateMessage(user, User.OnlineStatus.OFFLINE));
    }

    private void sendOnlineStatusUpdateMessage(User user, User.OnlineStatus status) {
        ResponseUpdateUserOnlineStatusDTO respUser = new ResponseUpdateUserOnlineStatusDTO(user.getId(), status);
        chatsRepository.findByUser1OrUser2(user, user).forEach(chat -> {
            messagingTemplate.convertAndSend("/topic/chat/" + chat.getWebSocketUUID(), respUser);
            User recipient = chat.getUser1().getId() != user.getId() ? chat.getUser1() : chat.getUser2();
            messagingTemplate.convertAndSend("/topic/user/" + recipient.getWebSocketUUID() + "/main", respUser);
        });
        user.getChannels().forEach(channelUser ->
                messagingTemplate.convertAndSend("/topic/channel/" + channelUser.getChannel().getWebSocketUUID(), respUser));
        user.getGroups().forEach(groupUser ->
                messagingTemplate.convertAndSend("/topic/group/" + groupUser.getGroup().getWebSocketUUID(), respUser));
    }

    public UserFriend findUserFriend(User user1, User user2) {
        Optional<UserFriend> userFriend = usersFriendsRepository.findByOwnerAndFriend(user1, user2);
        if (userFriend.isPresent()) {
            return userFriend.get();
        }
        Optional<UserFriend> userFriend2 = usersFriendsRepository.findByOwnerAndFriend(user2, user1);
        return userFriend2.orElse(null);
    }

    public User getById(int id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal();
        return userDetailsImpl.getUser();
    }
}
