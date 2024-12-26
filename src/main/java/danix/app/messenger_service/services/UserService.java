package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.util.ImageException;
import danix.app.messenger_service.util.ImageService;
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
public class UserService implements Image {
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

    @Value("${default_user_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${users_images_path}")
    private String USERS_IMAGES_PATH;

    public ShowUserDTO findUser(String username) {
        User user = usersRepository.findByUsername(username).orElse(null);
        if (user == null || user.getId() == getCurrentUser().getId()) {
            return null;
        }
        return modelMapper.map(user, ShowUserDTO.class);
    }

    public User getById(int id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public UserInfoDTO getUserInfo() {
        return modelMapper.map(getCurrentUser(), UserInfoDTO.class);
    }

    @Override
    public ResponseImageDTO getImage(int id) {
        User user = usersRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
        return ImageService.download(Path.of(USERS_IMAGES_PATH), user.getImageUUID());
    }

    @Override
    @Transactional
    public void addImage(MultipartFile imageFile, int id) {
        User currentUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
        String uuid = UUID.randomUUID().toString();
        ImageService.upload(Path.of(USERS_IMAGES_PATH), imageFile, uuid);
        if (currentUser.getImageUUID().equals(DEFAULT_IMAGE_UUID)) {
            currentUser.setImageUUID(uuid);
            return;
        }
        ImageService.delete(Path.of(USERS_IMAGES_PATH), currentUser.getImageUUID());
        currentUser.setImageUUID(uuid);
    }

    @Override
    @Transactional
    public void deleteImage(int id) {
        User currentUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
        if (currentUser.getImageUUID().equals(DEFAULT_IMAGE_UUID)) {
            throw new ImageException("User already have default image");
        }
        ImageService.delete(Path.of(USERS_IMAGES_PATH), currentUser.getImageUUID());
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

    public List<ResponseUserDTO> getAllFriendsRequests() {
        return usersFriendsRepository.findByFriend(getCurrentUser()).stream()
                .filter(userFriend -> userFriend.getStatus() == FriendsStatus.WAITING)
                .map(userFriend -> modelMapper.map(userFriend, ResponseUserDTO.class))
                .collect(Collectors.toList());
    }

    public List<ShowUserDTO> getAllUserFriends() {
        return usersFriendsRepository.findByOwnerOrFriend(getCurrentUser(), getCurrentUser()).stream()
                .filter(user -> user.getStatus() == FriendsStatus.ACCEPTED)
                .map(user -> modelMapper.map(user.getFriend(), ShowUserDTO.class))
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
        usersFriendsRepository.findByOwnerAndFriend(getCurrentUser(), user).ifPresentOrElse(userFriend -> {
            if (userFriend.getStatus() != FriendsStatus.WAITING) {
                throw new UserException("User is already in your friend list");
            }
            usersFriendsRepository.delete(userFriend);
        }, () -> {
            throw new UserException("Request not found");
        });
    }

    @Transactional
    public void deleteFriend(String username) {
        User friend = getByUsername(username);
        usersFriendsRepository.findByOwnerAndFriend(getCurrentUser(), friend)
                .ifPresentOrElse(usersFriendsRepository::delete, () -> usersFriendsRepository.findByOwnerAndFriend(friend, getCurrentUser())
                        .ifPresentOrElse(usersFriendsRepository::delete, () -> {
                            throw new UserException("User is not exist in your friends");
                        }));
    }

    @Transactional
    public void blockUser(int id) {
        User user = getById(id);
        blockedUsersRepository.findByOwnerAndBlockedUser(getCurrentUser(), user)
                .ifPresentOrElse(blockedUser -> {
                    throw new UserException("User is already blocked");
                }, () -> {
                    blockedUsersRepository.save(new BlockedUser(getCurrentUser(), user));
                    usersFriendsRepository.findByOwnerAndFriend(getCurrentUser(), user)
                            .ifPresentOrElse(usersFriendsRepository::delete, () -> usersFriendsRepository.findByOwnerAndFriend(user, getCurrentUser())
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
        usersFriendsRepository.findByOwnerAndFriend(user, getCurrentUser())
                .ifPresentOrElse(friendRequest -> {
                    if (friendRequest.getStatus() == FriendsStatus.ACCEPTED) {
                        throw new UserException("Request is already accepted");
                    }
                    friendRequest.setStatus(FriendsStatus.ACCEPTED);
                }, () -> {
                    throw new UserException("Request not found");
                });
    }

    @Transactional
    public void addFriend(String username) {
        User user = getByUsername(username);
        User currentUser = getCurrentUser();
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser)
                .ifPresentOrElse(blockedUser -> {
                    throw new UserException("The user has blocked you");
                }, () -> blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)
                        .ifPresent(blockedUsersRepository::delete));
        UserFriend userFriend = findUserFriend(user, currentUser);
        if (userFriend != null) {
            if (userFriend.getStatus() == FriendsStatus.WAITING) {
                throw new UserException("A request has already been sent to this user");
            }
            throw new UserException("Friend already exists");
        }
        usersFriendsRepository.save(new UserFriend(getCurrentUser(), user, FriendsStatus.WAITING));
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
    }

    @Transactional
    public void unBanUser(int id) {
        User user = getById(id);
        BannedUser bannedUser = bannedUsersRepository.findByUser(user)
                .orElseThrow(() -> new UserException("User is not banned"));
        user.setUserStatus(User.Status.REGISTERED);
        user.setBanned(false);
        bannedUsersRepository.delete(bannedUser);
    }

    @Transactional
    public void registerUser(String email) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found"));
        if (user.getUserStatus() != User.Status.TEMPORALLY_REGISTERED) {
            throw new UserException("User is already registered");
        }
        user.setUserStatus(User.Status.REGISTERED);
    }

    @Transactional
    public void updateUser(int id, User user) {
        user.setId(id);
        usersRepository.save(user);
    }

    @Transactional
    public void temporalRegister(RegistrationUserDTO personDTO) {
        usersRepository.save(
                User.builder()
                        .username(personDTO.getUsername())
                        .createdAt(LocalDateTime.now())
                        .description(personDTO.getDescription())
                        .email(personDTO.getEmail())
                        .role(User.Roles.ROLE_USER)
                        .password(passwordEncoder.encode(personDTO.getPassword()))
                        .isPrivate(personDTO.getIsPrivate() != null && personDTO.getIsPrivate())
                        .imageUUID(DEFAULT_IMAGE_UUID)
                        .userStatus(User.Status.TEMPORALLY_REGISTERED)
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
    public void updateOnlineStatus() {
        User user = getById(getCurrentUser().getId());
        switch (user.getOnlineStatus()) {
            case ONLINE:
                user.setOnlineStatus(User.OnlineStatus.OFFLINE);
                break;
            case OFFLINE:
                user.setOnlineStatus(User.OnlineStatus.ONLINE);
        }
        ResponseUpdateUserOnlineStatusDTO respUser = new ResponseUpdateUserOnlineStatusDTO(user.getId(), user.getOnlineStatus());
        user.getChats().forEach(chat -> {
            messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), respUser);
            int userId = chat.getUser1().getId() != user.getId() ? chat.getUser1().getId() : chat.getUser2().getId();
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/main", respUser);
        });
        user.getChannels().forEach(channelUser -> messagingTemplate.convertAndSend("/topic/channel/" + channelUser.getChannel().getId(), respUser));
        user.getGroups().forEach(groupUser -> messagingTemplate.convertAndSend("/topic/group/" + groupUser.getGroup().getId(), respUser));
    }

    private UserFriend findUserFriend(User user1, User user2) {
        Optional<UserFriend> userFriend = usersFriendsRepository.findByOwnerAndFriend(user1, user2);
        if (userFriend.isPresent()) {
            return userFriend.get();
        }
        Optional<UserFriend> userFriend2 = usersFriendsRepository.findByOwnerAndFriend(user2, user1);
        return userFriend2.orElse(null);
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal();
        return userDetailsImpl.getUser();
    }
}
