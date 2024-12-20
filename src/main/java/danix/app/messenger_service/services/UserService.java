package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.security.PersonDetails;
import danix.app.messenger_service.util.ImageException;
import danix.app.messenger_service.util.UserException;
import org.modelmapper.ModelMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
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
public class UserService implements Image {
    private final UsersRepository usersRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokensService tokensService;
    private final BannedUsersRepository bannedUsersRepository;
    private final UsersFriendsRepository usersFriendsRepository;
    private final BlockedUsersRepository blockedUsersRepository;
    private final AppMessagesRepository appMessagesRepository;
    private final GroupsService groupsService;
    private final ChatsService chatsService;
    private final EmailsKeysRepository emailsKeysRepository;
    private final ChannelsService channelsService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${default_user_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${users_images_path}")
    private String USERS_IMAGES_PATH;

    @Autowired
    public UserService(UsersRepository usersRepository, ModelMapper modelMapper,
                       @Lazy PasswordEncoder passwordEncoder, TokensService tokensService,
                       BannedUsersRepository bannedUsersRepository, UsersFriendsRepository usersFriendsRepository,
                       BlockedUsersRepository blockedUsersRepository, AppMessagesRepository appMessagesRepository,
                       @Lazy GroupsService groupsService, @Lazy ChatsService chatsService, EmailsKeysRepository emailsKeysRepository,
                       @Lazy ChannelsService channelsService, KafkaTemplate<String, String> kafkaTemplate) {
        this.usersRepository = usersRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokensService = tokensService;
        this.bannedUsersRepository = bannedUsersRepository;
        this.usersFriendsRepository = usersFriendsRepository;
        this.blockedUsersRepository = blockedUsersRepository;
        this.appMessagesRepository = appMessagesRepository;
        this.groupsService = groupsService;
        this.chatsService = chatsService;
        this.emailsKeysRepository = emailsKeysRepository;
        this.channelsService = channelsService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public ResponseUserDTO findUser(String username) {
        return convertToResponsePersonDTO(usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found")));
    }

    public User getById(int id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public UserInfoDTO getUserInfo() {
        User currentUser = getCurrentUser();
        return UserInfoDTO.builder()
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .appMessages(getUserMessages())
                .groups(groupsService.getAllUserGroups())
                .groupInvites(groupsService.getAllUserGroupsInvites().stream()
                        .map(this::convertToResponseGroupInviteDTO)
                        .toList())
                .channels(channelsService.getAllUserChannels())
                .channelInvites(channelsService.getChannelsInvites())
                .friends(getAllUserFriends())
                .chats(chatsService.getAllUserChats())
                .build();
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

    private ResponseGroupInviteDTO convertToResponseGroupInviteDTO(GroupInvite groupInvite) {
        ResponseGroupInviteDTO responseGroupInviteDTO = new ResponseGroupInviteDTO();
        responseGroupInviteDTO.setGroupId(groupInvite.getGroup().getId());
        responseGroupInviteDTO.setGroupName(groupInvite.getGroup().getName());
        responseGroupInviteDTO.setGroupName(groupInvite.getGroup().getName());
        responseGroupInviteDTO.setInvitedAt(groupInvite.getSentTime());
        return responseGroupInviteDTO;
    }

    public User getByEmail(String email) {
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public User getByUsername(String username) {
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public List<ResponseAppMessageDTO> getUserMessages() {
        User currentUser = getCurrentUser();
        return appMessagesRepository.findByUser(currentUser).stream()
                .map(msg -> modelMapper.map(msg, ResponseAppMessageDTO.class))
                .collect(Collectors.toList());
    }

    public List<ResponseUserDTO> getAllFriendsRequests() {
        return usersFriendsRepository.findByFriend(getCurrentUser()).stream()
                .filter(userFriend -> userFriend.getStatus() == FriendsStatus.WAITING)
                .map(userFriend -> convertToResponsePersonDTO(userFriend.getOwner()))
                .collect(Collectors.toList());
    }

    public List<ResponseUserDTO> getAllUserFriends() {
        return usersFriendsRepository.findByOwnerOrFriend(getCurrentUser(), getCurrentUser()).stream()
                .filter(friend -> friend.getStatus() == FriendsStatus.ACCEPTED)
                .map(friend -> convertToResponsePersonDTO(friend.getFriend()))
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
    public void cancelFriendRequest(String username) {
        User user = getByUsername(username);
        usersFriendsRepository.findByOwnerAndFriend(getCurrentUser(), user)
                .ifPresentOrElse(userFriend -> {
                    if (userFriend.getStatus() != FriendsStatus.WAITING) {
                        throw new UserException("The person is already a friend of the user");
                    }
                    usersFriendsRepository.delete(userFriend);
                }, () -> {
                    throw new UserException("User did not send a request to this person");
                });
    }

    @Transactional
    public void deleteFriend(String username) {
        User friend = getByUsername(username);
        usersFriendsRepository.findByOwnerAndFriend(getCurrentUser(), friend)
                .ifPresentOrElse(usersFriendsRepository::delete, () -> {
                    usersFriendsRepository.findByOwnerAndFriend(friend, getCurrentUser())
                            .ifPresentOrElse(usersFriendsRepository::delete, () -> {
                                throw new UserException("User is not exist in your friends");
                            });
                });
    }

    @Transactional
    public void blockUser(String username) {
        User user = getByUsername(username);
        blockedUsersRepository.findByOwnerAndBlockedUser(getCurrentUser(), user)
                .ifPresentOrElse(blockedUser -> {
                    throw new UserException("User is already blocked");
                }, () -> {
                    blockedUsersRepository.save(new BlockedUser(getCurrentUser(), user));
                    usersFriendsRepository.findByOwnerAndFriend(getCurrentUser(), user)
                            .ifPresentOrElse(usersFriendsRepository::delete, () -> {
                                usersFriendsRepository.findByOwnerAndFriend(user, getCurrentUser())
                                        .ifPresent(usersFriendsRepository::delete);
                            });
                });
    }

    @Transactional
    public void unblockUser(String username) {
        User user = getByUsername(username);
        blockedUsersRepository.findByOwnerAndBlockedUser(getCurrentUser(), user)
                .ifPresentOrElse(blockedUsersRepository::delete, () -> {
                    throw new UserException("User is not blocked");
                });
    }

    @Transactional
    public void acceptFriend(String username) {
        User user = getByUsername(username);
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
                }, () -> {
                    blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user)
                            .ifPresent(blockedUsersRepository::delete);
                });
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
    public void banUser(BanUserDTO banUserDTO) {
        User user = getByUsername(banUserDTO.getUsername());
        if (user.getUserStatus() == User.Status.BANNED) {
            throw new UserException("User is already banned");
        }
        user.setUserStatus(User.Status.BANNED);
        tokensService.getAllUserTokens(user).forEach(token -> token.setStatus(TokenStatus.REVOKED));
        bannedUsersRepository.save(new BannedUser(banUserDTO.getReason(), user));
    }

    @Transactional
    public void unBanUser(String username) {
        User user = getByUsername(username);
        BannedUser bannedUser = bannedUsersRepository.findByUser(user)
                .orElseThrow(() -> new UserException("User is not banned"));
        user.setUserStatus(User.Status.REGISTERED);
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

    UserFriend findUserFriend(User user1, User user2) {
        Optional<UserFriend> userFriend = usersFriendsRepository.findByOwnerAndFriend(user1, user2);
        if (userFriend.isPresent()) {
            return userFriend.get();
        }
        Optional<UserFriend> userFriend2 = usersFriendsRepository.findByOwnerAndFriend(user2, user1);
        return userFriend2.orElse(null);

    }

    private ResponseUserDTO convertToResponsePersonDTO(User user) {
        return modelMapper.map(user, ResponseUserDTO.class);
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        return personDetails.getUser();
    }
}
