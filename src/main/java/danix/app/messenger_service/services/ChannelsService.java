package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.ChannelException;
import danix.app.messenger_service.util.FileException;
import danix.app.messenger_service.util.FilesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelsService {
    private final ChannelsRepository channelsRepository;
    private final ChannelsUsersRepository channelsUsersRepository;
    private final ChannelsInvitesRepository channelsInvitesRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final ChannelsLogsRepository channelsLogsRepository;
    private final AppMessagesRepository appMessagesRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BlockedUsersRepository blockedUsersRepository;
    private final ChannelsPostsRepository channelsPostsRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChannelsPostsCommentsRepository postsCommentsRepository;
    private final ChannelsPostsFilesRepository postsFilesRepository;

    @Value("${default_channels_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${channels_avatars_path}")
    private String AVATARS_PATH;
    @Value("${channels_posts_images_path}")
    private String POSTS_IMAGES_PATH;
    @Value("${channels_posts_comments_images_path}")
    private String COMMENTS_IMAGES_PATH;
    @Value("${channels_posts_comments_videos_path}")
    private String COMMENTS_VIDEOS_PATH;
    @Value("${channels_posts_videos_path}")
    private String POSTS_VIDEOS_PATH;
    @Value("${channels_posts_comments_audio_path}")
    private String COMMENTS_AUDIO_PATH;
    @Value("${channels_posts_audio_path}")
    private String POSTS_AUDIO_PATH;

    public List<ResponseChannelDTO> getAllUserChannels() {
        User currentUser = userService.getById(getCurrentUser().getId());
        return currentUser.getChannels().stream()
                .filter(user -> !user.getChannel().isBaned())
                .map(user -> modelMapper.map(user.getChannel(), ResponseChannelDTO.class))
                .collect(Collectors.toList());
    }

    public ResponseChannelDTO findChannel(String name) {
        Channel channel = channelsRepository.findByNameStartsWith(name)
                .orElse(null);
        if (channel == null) {
            return null;
        }
        if (!channel.isPrivate() && !channel.isBaned() || getCurrentUser().getRole() == User.Roles.ROLE_ADMIN) {
            return modelMapper.map(channel, ResponseChannelDTO.class);
        }
        return null;
    }

    public List<ResponseChannelInviteDTO> getChannelsInvites() {
        return channelsInvitesRepository.findAllByUser(getCurrentUser()).stream()
                .map(invite -> modelMapper.map(invite, ResponseChannelInviteDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public long createChannel(CreateChannelDTO createChannelDTO) {
        channelsRepository.findByName(createChannelDTO.getName()).ifPresent(channel -> {
            throw new ChannelException("Channel with this name already exists");
        });
        Channel channel = Channel.builder()
                .name(createChannelDTO.getName())
                .createdAt(new Date())
                .description(createChannelDTO.getDescription())
                .owner(getCurrentUser())
                .image(DEFAULT_IMAGE_UUID)
                .isPrivate(createChannelDTO.getIsPrivate() != null && createChannelDTO.getIsPrivate())
                .webSocketUUID(UUID.randomUUID().toString())
                .isPostsCommentsAllowed(true)
                .isFilesAllowed(true)
                .isInvitesAllowed(true)
                .build();

        ChannelUser channelUser = new ChannelUser();
        channelUser.setChannel(channel);
        channelUser.setUser(getCurrentUser());
        channelUser.setIsAdmin(true);
        channelsRepository.save(channel);
        channelsUsersRepository.save(channelUser);
        return channel.getId();
    }

    @Transactional
    public void addImage(MultipartFile image, int channelId) {
        Channel channel = getById(channelId);
        if (channel.getOwner().getId() != getCurrentUser().getId()) {
            throw new ChannelException("Current user must be owner of channel");
        }
        String uuid = UUID.randomUUID().toString();
        FilesUtils.upload(Path.of(AVATARS_PATH), image, uuid, ContentType.IMAGE);
        if (channel.getImage().equals(DEFAULT_IMAGE_UUID)) {
            channel.setImage(uuid);
            return;
        }
        FilesUtils.delete(Path.of(AVATARS_PATH), channel.getImage());
        channel.setImage(uuid);
        sendUpdateChannelMessage(channel, true);
    }

    public ResponseFileDTO getImage(int channelId) {
        Channel channel = getById(channelId);
        getChannelUser(getCurrentUser(), channel);
        return FilesUtils.download(Path.of(AVATARS_PATH), channel.getImage(), ContentType.IMAGE);
    }

    @Transactional
    public void deleteImage(int channelId) {
        Channel channel = getById(channelId);
        if (channel.getOwner().getId() != getCurrentUser().getId()) {
            throw new FileException("Current user must be owner of channel");
        }
        if (channel.getImage().equals(DEFAULT_IMAGE_UUID)) {
            throw new FileException("Channel already have default image");
        }
        FilesUtils.delete(Path.of(AVATARS_PATH), channel.getImage());
        channel.setImage(DEFAULT_IMAGE_UUID);
        sendUpdateChannelMessage(channel, true);
    }

    @Transactional
    public void inviteToChannel(int channelId, int userId) {
        Channel channel = getById(channelId);
        User user = userService.getById(userId);
        User currentUser = getCurrentUser();
        getChannelUser(currentUser, channel);
        if (!channel.isInvitesAllowed() && channel.getOwner().getId() != currentUser.getId()) {
            throw new ChannelException("Invites are not allowed in this channel");
        }
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresent(blockedUser -> {
            throw new ChannelException("Current user are blocked from this user");
        });
        blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(blockedUser -> {
            throw new ChannelException("User has blocked by current user");
        });
        if (user.getIsPrivate()) {
            if (userService.findUserFriend(user, currentUser) == null) {
                throw new ChannelException("User has a private account");
            }
        }
        channelsInvitesRepository.findByUserAndChannel(user, channel).ifPresent(invite -> {
            throw new ChannelException("User already has invite to this channel");
        });
        channelsUsersRepository.findByUserAndChannel(user, channel).ifPresent(channelUser -> {
            throw new ChannelException("User already exist in this channel");
        });
        if (channel.getBannedUsers().contains(user)) {
            throw new ChannelException("User banned in this channel");
        }
        ChannelInvite channelInvite = new ChannelInvite();
        channelInvite.setChannel(channel);
        channelInvite.setUser(user);
        channelInvite.setSendTime(LocalDateTime.now());
        channelInvite.setExpiredTime(LocalDateTime.now().plusDays(2));
        channelsInvitesRepository.save(channelInvite);
    }

    @Transactional
    public CompletableFuture<Void> leaveChannel(int id) {
        Channel channel = getById(id);
        User currentUser = getCurrentUser();
        ChannelUser currentChannelUser = channelsUsersRepository.findByUserAndChannel(currentUser, channel)
                .orElseThrow(() -> new ChannelException("Current user not exist in this channel"));
        channelsUsersRepository.delete(currentChannelUser);
        if (channel.getOwner().getId() == currentUser.getId()) {
            return deleteChannel(id);
        }
        return null;
    }

    @Transactional
    public void banChannel(int id, String reason) {
        Channel channel = getById(id);
        if (channel.isBaned()) throw new ChannelException("Channel is already banned");
        channel.setBaned(true);
        kafkaTemplate.send("ban_channel-topic", channel.getOwner().getEmail(),
                "Your channel with name " + channel.getName() + " has been banned for reason: " + reason);
        AppMessage appMessage = new AppMessage(
                "Your channel with name " + channel.getName() + " has been banned for reason: " + reason,
                channel.getOwner()
        );
        appMessagesRepository.save(appMessage);
        messagingTemplate.convertAndSend("/topic/user/" + channel.getOwner().getWebSocketUUID() + "/main",
                new ResponseAppMessageDTO(appMessage.getMessage(), appMessage.getSentTime()));
        messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                Map.of("deleted", true));
    }

    @Transactional
    public void unbanChannel(int id) {
        Channel channel = getById(id);
        if (!channel.isBaned()) throw new ChannelException("Channel is not banned");
        channel.setBaned(false);
        kafkaTemplate.send("unban_channel-topic", channel.getOwner().getEmail(),
                "Your channel with name " + channel.getName() + " has been unbanned!");
        AppMessage appMessage = new AppMessage(
                "Your channel with name " + channel.getName() + " has been unbanned!",
                channel.getOwner()
        );
        appMessagesRepository.save(appMessage);
        messagingTemplate.convertAndSend("/topic/user/" + channel.getOwner().getWebSocketUUID() + "/main",
                new ResponseAppMessageDTO(appMessage.getMessage(), appMessage.getSentTime()));
    }

    @Transactional
    public void acceptInviteToChannel(int id) {
        Channel channel = getById(id);
        User currentUser = userService.getById(getCurrentUser().getId());
        ChannelInvite invite = channelsInvitesRepository.findByUserAndChannel(currentUser, channel)
                .orElseThrow(() -> new ChannelException("Invite not found"));
        channelsUsersRepository.findByUserAndChannel(getCurrentUser(), channel).ifPresent(user -> {
            throw new ChannelException("Current user already exist in this channel");
        });
        if (channel.getBannedUsers().contains(currentUser)) {
            throw new ChannelException("Current user banned in this channel");
        }
        ChannelUser channelUser = new ChannelUser();
        channelUser.setChannel(channel);
        channelUser.setUser(currentUser);
        channelUser.setIsAdmin(false);
        channelsUsersRepository.save(channelUser);
        channelsInvitesRepository.delete(invite);
    }

    public List<ResponseUserDTO> getBannedUsers(int channelId) {
        User currentUser = getCurrentUser();
        Channel channel = getById(channelId);
        ChannelUser channelUser = getChannelUser(currentUser, channel);
        if (!channelUser.getIsAdmin()) {
            throw new ChannelException("Current user is not admin of the channel");
        }
        return channel.getBannedUsers().stream()
                .map(bannedUser -> modelMapper.map(bannedUser, ResponseUserDTO.class))
                .toList();
    }

    @Transactional
    public void banUser(int channelId, int userId) {
        Channel channel = getById(channelId);
        User currentUser = getCurrentUser();
        ChannelUser channelUser = getChannelUser(currentUser, channel);
        User user = userService.getById(userId);
        if (!channelUser.getIsAdmin()) {
            throw new ChannelException("Current user is not admin");
        }
        if (channel.getBannedUsers().contains(user)) {
            throw new ChannelException("User is already banned");
        }
        channelsUsersRepository.findByUserAndChannel(user, channel).ifPresent(foundUser -> {
            if (foundUser.getIsAdmin() && channel.getOwner().getId() != currentUser.getId()) {
                throw new ChannelException("Current user can't ban admin");
            }
            channelsUsersRepository.delete(foundUser);
            AppMessage appMessage = new AppMessage("You were banned in channel - " + channel.getName(), user);
            appMessagesRepository.save(appMessage);
            messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/main",
                    new ResponseAppMessageDTO(appMessage.getMessage(), appMessage.getSentTime()));
            messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/main",
                    Map.of("deleted_channel", channelId));
        });
        channel.getBannedUsers().add(user);
        ChannelLog channelLog = new ChannelLog();
        channelLog.setChannel(channel);
        channelLog.setMessage(channelUser.getUsername() + " banned user - " + user.getUsername());
        channelsLogsRepository.save(channelLog);
    }

    @Transactional
    public void unbanUser(int channelId, int userId) {
        Channel channel = getById(channelId);
        User currentUser = getCurrentUser();
        ChannelUser channelUser = getChannelUser(currentUser, channel);
        User user = userService.getById(userId);
        if (!channelUser.getIsAdmin()) {
            throw new ChannelException("Current user is not admin");
        }
        if (channel.getBannedUsers().contains(user)) {
            channel.getBannedUsers().remove(user);
            ChannelLog channelLog = new ChannelLog();
            channelLog.setChannel(channel);
            channelLog.setMessage(currentUser.getUsername() + " unbanned user - " + user.getUsername());
            channelsLogsRepository.save(channelLog);
        } else {
            throw new ChannelException("User is not banned");
        }
    }

    @Transactional
    public void addAdmin(int channelId, int userId) {
        Channel channel = getById(channelId);
        User currentUser = getCurrentUser();
        if (channel.getOwner().getId() == currentUser.getId()) {
            User user = userService.getById(userId);
            ChannelUser channelUser = getChannelUser(user, channel);
            if (channelUser.getIsAdmin()) {
                throw new ChannelException("User is already admin");
            }
            channelUser.setIsAdmin(true);
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(currentUser.getUsername() + " added new admin " + user.getUsername());
            channelLog.setChannel(channel);
            channelsLogsRepository.save(channelLog);
        } else {
            throw new ChannelException("Current user is not owner of this channel");
        }
    }

    @Transactional
    public void deleteAdmin(int channelId, int userId) {
        Channel channel = getById(channelId);
        User user = userService.getById(userId);
        ChannelUser channelUser = getChannelUser(user, channel);
        User currentUser = getCurrentUser();
        if (channel.getOwner().getId() == currentUser.getId()) {
            if (!channelUser.getIsAdmin()) {
                throw new ChannelException("User is not admin");
            }
            channelUser.setIsAdmin(false);
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(currentUser.getUsername() + " deleted admin " + user.getUsername());
            channelLog.setChannel(channel);
            channelsLogsRepository.save(channelLog);
        } else {
            throw new ChannelException("Current user is not owner of this channel");
        }
    }

    @Transactional
    public void updateChannel(UpdateChannelDTO updateChannelDTO, int id) {
        Channel channel = getById(id);
        if (channel.getOwner().getId() == getCurrentUser().getId()) {
            channel.setName(updateChannelDTO.getName() == null ? channel.getName() : updateChannelDTO.getName());
            channel.setDescription(updateChannelDTO.getDescription() == null ? channel.getDescription() : updateChannelDTO.getDescription());
            sendUpdateChannelMessage(channel, false);
        } else {
            throw new ChannelException("Current user is not owner of this channel");
        }
    }

    @Transactional
    public void updateChannelOptions(int id, ChannelsOptionsDTO options) {
        Channel channel = getById(id);
        if (channel.getOwner().getId() == getCurrentUser().getId()) {
            channel.setPrivate(options.getIsPrivate() != null && options.getIsPrivate());
            channel.setFilesAllowed(options.getIsFilesAllowed() != null ? options.getIsFilesAllowed()
                    : channel.isFilesAllowed());
            channel.setPostsCommentsAllowed(options.getIsPostsCommentsAllowed() != null ? options.getIsPostsCommentsAllowed()
                    : channel.isPostsCommentsAllowed());
            channel.setInvitesAllowed(options.getIsInvitesAllowed() != null ? options.getIsInvitesAllowed()
                    : channel.isInvitesAllowed());
        } else {
            throw new ChannelException("Current user must be owner of channel");
        }
    }

    @Transactional
    public CompletableFuture<Void> deleteChannel(int id) {
        Channel channel = getById(id);
        User currentUser = getCurrentUser();
        if (channel.getOwner().getId() == currentUser.getId()) {
            if (channel.getImage() != null && !channel.getImage().equals(DEFAULT_IMAGE_UUID)) {
                FilesUtils.delete(Path.of(AVATARS_PATH), channel.getImage());
            }
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            return CompletableFuture.runAsync(() -> {
                AtomicInteger postsPage = new AtomicInteger();
                while (true) {
                    List<ChannelPost> posts = channelsPostsRepository.findAllByChannel(channel,
                            PageRequest.of(postsPage.get(), 50));
                    if (posts.isEmpty()) {
                        break;
                    }
                    CompletableFuture<Void> deleteFilesTask = CompletableFuture.runAsync(() -> postsFilesRepository
                          .findAllByPostIn(posts).forEach(file -> ChannelsPostsService.deletePostFile(file, POSTS_IMAGES_PATH,
                                  POSTS_VIDEOS_PATH, POSTS_AUDIO_PATH)), executorService);
                    CompletableFuture<Void> deleteCommentsTask = CompletableFuture.runAsync(() -> {
                        for (ChannelPost post : posts) {
                            int commentsPage = 0;
                            List<ChannelPostComment> comments;
                            do {
                                comments = postsCommentsRepository.findAllByPostAndContentTypeIsNot(post, ContentType.TEXT,
                                        PageRequest.of(commentsPage, 50));
                                comments.forEach(comment -> ChannelsPostsService.deleteCommentFile(comment, COMMENTS_IMAGES_PATH,
                                        COMMENTS_VIDEOS_PATH, COMMENTS_AUDIO_PATH));
                                commentsPage++;
                            } while (!comments.isEmpty());
                        }
                    }, executorService);
                    CompletableFuture.allOf(deleteFilesTask, deleteCommentsTask).join();
                    postsPage.incrementAndGet();
                }
                channelsRepository.deleteById(channel.getId());
                messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                        Map.of("deleted", true));
            }, executorService);
        } else {
            throw new ChannelException("Current user must be owner of channel");
        }
    }

    public List<ResponseChannelUserDTO> getUsers(int channelId, int page, int count) {
        Channel channel = getById(channelId);
        getChannelUser(getCurrentUser(), channel);
        return channelsUsersRepository.findByChannel(channel, PageRequest.of(page, count)).stream()
                .map(channelUser -> {
                    ResponseChannelUserDTO respUser = new ResponseChannelUserDTO();
                    respUser.setId(channelUser.getUser().getId());
                    respUser.setUsername(channelUser.getUser().getUsername());
                    respUser.setIsAdmin(channelUser.getIsAdmin());
                    return respUser;
                })
                .toList();
    }

    public ShowChannelDTO showChannel(int id) {
        User currentUser = getCurrentUser();
        Channel channel = getById(id);
        getChannelUser(currentUser, channel);
        return ShowChannelDTO.builder()
                .owner(modelMapper.map(currentUser, ResponseUserDTO.class))
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .createdAt(channel.getCreatedAt())
                .webSocketUUID(channel.getWebSocketUUID())
                .usersCount(channelsUsersRepository.countByChannel(channel))
                .build();
    }

    public List<ResponseChannelLogDTO> showChannelLogs(int id) {
        User currentUser = getCurrentUser();
        Channel channel = getById(id);
        if (channel.getOwner().getId() != currentUser.getId()) {
            throw new ChannelException("Current user is not owner of this channel");
        }
        return channel.getLogs().stream()
                .map(log -> modelMapper.map(log, ResponseChannelLogDTO.class))
                .toList();
    }

    @Transactional
    public void joinToChannel(int id) {
        Channel channel = getById(id);
        User currentUser = userService.getById(getCurrentUser().getId());
        channelsUsersRepository.findByUserAndChannel(currentUser, channel).ifPresent(user -> {
            throw new ChannelException("Current user already exist in this channel");
        });
        if (channel.isPrivate()) {
            throw new ChannelException("Channel is private");
        }
        if (channel.getBannedUsers().contains(currentUser)) {
            throw new ChannelException("Current user banned in this channel");
        }
        ChannelUser channelUser = new ChannelUser();
        channelUser.setChannel(channel);
        channelUser.setUser(currentUser);
        channelUser.setIsAdmin(false);
        channelsUsersRepository.save(channelUser);
    }

    public ChannelsOptionsDTO getChannelOptions(int id) {
        Channel channel = getById(id);
        if (channel.getOwner().getId() == getCurrentUser().getId()) {
            return modelMapper.map(channel, ChannelsOptionsDTO.class);
        }
        throw new ChannelException("Current user must be owner of channel");
    }

    public Channel getById(int id) {
        Channel channel = channelsRepository.findById(id)
                .orElseThrow(() -> new ChannelException("Channel not found"));
        if (channel.isBaned() && getCurrentUser().getRole() != User.Roles.ROLE_ADMIN) {
            throw new ChannelException("Channel not found");
        }
        return channel;
    }

    public ChannelUser getChannelUser(User user, Channel channel) {
        return channelsUsersRepository.findByUserAndChannel(user, channel)
                .orElseThrow(() -> new ChannelException("Current user not exist in this channel"));
    }

    private void sendUpdateChannelMessage(Channel channel, boolean imageUpdated) {
        ResponseChannelUpdatingDTO response =
                new ResponseChannelUpdatingDTO(modelMapper.map(channel, ResponseChannelDTO.class), imageUpdated);
        messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(), response);
    }
}