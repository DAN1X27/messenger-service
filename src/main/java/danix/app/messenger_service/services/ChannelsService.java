package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.ChannelException;
import danix.app.messenger_service.util.FileException;
import danix.app.messenger_service.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

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
    private final ChannelsPostsFilesRepository postFilesRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${default_channels_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${channels_avatars_path}")
    private String AVATARS_PATH;
    @Value("${channels_posts_images_path}")
    private String POSTS_IMAGES_PATH;
    @Value("${channels_posts_comments_images_path}")
    private String POSTS_COMMENTS_IMAGES_PATH;
    @Value("${channels_posts_comments_videos_path}")
    private String POSTS_COMMENTS_VIDEOS_PATH;
    @Value("${channels_posts_videos_path}")
    private String POSTS_VIDEOS_PATH;
    @Value("${channels_posts_comments_audio_path}")
    private String POSTS_COMMENTS_AUDIO_PATH;
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
                .map(this::convertToInviteDTO)
                .collect(Collectors.toList());
    }

    private ResponseChannelInviteDTO convertToInviteDTO(ChannelInvite invite) {
        ResponseChannelInviteDTO inviteDTO = new ResponseChannelInviteDTO();
        inviteDTO.setChannelName(invite.getChannel().getName());
        inviteDTO.setSendTime(invite.getSendTime());
        inviteDTO.setChannelId(invite.getChannel().getId());
        return inviteDTO;
    }

    @Transactional
    public void createChannel(CreateChannelDTO createChannelDTO) {
        channelsRepository.findByName(createChannelDTO.getName()).ifPresentOrElse(channel -> {
            throw new ChannelException("Channel with this name already exists");
        }, () -> {
            Channel channel = Channel.builder()
                    .name(createChannelDTO.getName())
                    .createdAt(new Date())
                    .description(createChannelDTO.getDescription())
                    .owner(getCurrentUser())
                    .image(DEFAULT_IMAGE_UUID)
                    .isPrivate(createChannelDTO.getIsPrivate() != null && createChannelDTO.getIsPrivate())
                    .webSocketUUID(UUID.randomUUID().toString())
                    .build();

            ChannelUser channelUser = new ChannelUser();
            channelUser.setChannel(channel);
            channelUser.setUser(getCurrentUser());
            channelUser.setIsAdmin(true);
            channelsRepository.save(channel);
            channelsUsersRepository.save(channelUser);
        });
    }

    @Transactional
    public void addImage(MultipartFile image, int channelId) {
        Channel channel = getById(channelId);
        if (channel.getOwner().getId() != getCurrentUser().getId()) {
            throw new ChannelException("Current user must be owner of channel");
        }
        String uuid = UUID.randomUUID().toString();
        FileUtils.upload(Path.of(AVATARS_PATH), image, uuid, ContentType.IMAGE);
        if (channel.getImage().equals(DEFAULT_IMAGE_UUID)) {
            channel.setImage(uuid);
            return;
        }
        FileUtils.delete(Path.of(AVATARS_PATH), channel.getImage());
        channel.setImage(uuid);
        for (ChannelUser channelUser : channel.getUsers()) {
            messagingTemplate.convertAndSend("/topic/user/" + channelUser.getUser().getWebSocketUUID() + "/main",
                    new ResponseChannelUpdatingDTO(modelMapper.map(channel, ResponseChannelDTO.class), true));
        }
    }

    public ResponseFileDTO getImage(int channelId) {
        Channel channel = getById(channelId);
        getChannelUser(getCurrentUser(), channel);
        return FileUtils.download(Path.of(AVATARS_PATH), channel.getImage(), ContentType.IMAGE);
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
        FileUtils.delete(Path.of(AVATARS_PATH), channel.getImage());
        channel.setImage(DEFAULT_IMAGE_UUID);
        for (ChannelUser channelUser : channel.getUsers()) {
            messagingTemplate.convertAndSend("/topic/user/" + channelUser.getUser().getWebSocketUUID() + "/main",
                    new ResponseChannelUpdatingDTO(modelMapper.map(channel, ResponseChannelDTO.class), true));
        }
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
    public void leaveChannel(int id) {
        Channel channel = getById(id);
        User currentUser = getCurrentUser();
        ChannelUser channelUser = channelsUsersRepository.findByUserAndChannel(currentUser, channel)
                .orElseThrow(() -> new ChannelException("Current user not exist in this channel"));
        channelsUsersRepository.delete(channelUser);
        if (channel.getOwner().getId() == currentUser.getId()) {
            for (ChannelUser channelUser1 : channel.getUsers()) {
                messagingTemplate.convertAndSend("/topic/user/" + channelUser1.getUser().getWebSocketUUID() + "/main",
                        new ResponseChannelDeletionDTO(id));
            }
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                    new ResponseChannelDeletionDTO(id));

            channelsRepository.delete(channel);
        }
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
        for (ChannelUser channelUser : channel.getUsers()) {
            messagingTemplate.convertAndSend("/topic/user/" + channelUser.getUser().getWebSocketUUID() + "/main",
                    new ResponseChannelDeletionDTO(channel.getId()));
        }
        messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                new ResponseChannelDeletionDTO(channel.getId()));
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
        ChannelInvite invite = channelsInvitesRepository.findByUserAndChannel(getCurrentUser(), channel)
                .orElseThrow(() -> new ChannelException("Invite not found"));
        channelsUsersRepository.findByUserAndChannel(getCurrentUser(), channel).ifPresent(user -> {
            throw new ChannelException("Current user already exist in this channel");
        });
        if (channel.getBannedUsers().contains(currentUser)) {
            throw new ChannelException("Current user banned in this channel");
        }
        ChannelUser channelUser = new ChannelUser();
        channelUser.setChannel(channel);
        channelUser.setUser(getCurrentUser());
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
                .collect(Collectors.toList());
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
            messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID(),
                    new ResponseAppMessageDTO(appMessage.getMessage(), appMessage.getSentTime()));
            messagingTemplate.convertAndSend("/topic/user/" + getCurrentUser().getWebSocketUUID() + "/main",
                    new ResponseChannelDeletionDTO(channelId));
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                    Map.of("deleted_user_id", userId));
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
            for (ChannelUser channelUser : channel.getUsers()) {
                messagingTemplate.convertAndSend("/topic/user/" + channelUser.getUser().getWebSocketUUID() + "/main",
                        new ResponseChannelUpdatingDTO(modelMapper.map(channel, ResponseChannelDTO.class), false));
            }
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
    public void deleteChannel(int id) {
        Channel channel = getById(id);
        User currentUser = getCurrentUser();
        if (channel.getOwner().getId() == currentUser.getId()) {
            for (ChannelUser channelUser : channel.getUsers()) {
                messagingTemplate.convertAndSend("/topic/user/" + channelUser.getUser().getWebSocketUUID() + "/main",
                        new ResponseChannelDeletionDTO(id));
            }
            if (channel.getImage() != null && !channel.getImage().equals(DEFAULT_IMAGE_UUID)) {
                FileUtils.delete(Path.of(AVATARS_PATH), channel.getImage());
            }
            List<ChannelPost> channelPosts = channel.getPosts();
            List<ChannelPostComment> postComments = postsCommentsRepository.findAllByPostIn(channelPosts);
            for (ChannelPostComment postComment : postComments) {
                switch (postComment.getContentType()) {
                    case IMAGE -> FileUtils.delete(Path.of(POSTS_COMMENTS_IMAGES_PATH), postComment.getText());
                    case VIDEO -> FileUtils.delete(Path.of(POSTS_COMMENTS_VIDEOS_PATH), postComment.getText());
                    case AUDIO_MP3, AUDIO_OGG -> FileUtils.delete(Path.of(POSTS_COMMENTS_AUDIO_PATH), postComment.getText());
                }
            }
            List<ChannelPostFile> files = postFilesRepository.findAllByPostIn(channelPosts);
            for (ChannelPostFile file : files) {
                switch (file.getContentType()) {
                    case IMAGE -> FileUtils.delete(Path.of(POSTS_IMAGES_PATH), file.getFileUUID());
                    case VIDEO -> FileUtils.delete(Path.of(POSTS_VIDEOS_PATH), file.getFileUUID());
                    case AUDIO_OGG, AUDIO_MP3 -> FileUtils.delete(Path.of(POSTS_AUDIO_PATH), file.getFileUUID());
                }
            }
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                    new ResponseChannelDeletionDTO(id));
            jdbcTemplate.update("DELETE FROM channels WHERE id = ?", id);
        } else {
            throw new ChannelException("Current user must be owner of channel");
        }
    }

    public ShowChannelDTO showChannel(int id, int postsPage, int postsCount) {
        User currentUser = getCurrentUser();
        Channel channel = getById(id);
        getChannelUser(currentUser, channel);
        return ShowChannelDTO.builder()
                .owner(modelMapper.map(currentUser, ResponseUserDTO.class))
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .createdAt(channel.getCreatedAt())
                .posts(channelsPostsRepository.findAllByChannel(channel, PageRequest.of(postsPage, postsCount)).stream()
                        .map(this::convertToResponseChannelPostsDTO)
                        .toList())
                .users(channel.getUsers().stream()
                        .map(this::convertToResponseChannelUserDTO)
                        .toList())
                .webSocketUUID(channel.getWebSocketUUID())
                .build();
    }

    private ResponseChannelUserDTO convertToResponseChannelUserDTO(ChannelUser channelUser) {
        ResponseChannelUserDTO user = new ResponseChannelUserDTO();
        user.setId(channelUser.getUser().getId());
        user.setUsername(channelUser.getUser().getUsername());
        user.setIsAdmin(channelUser.getIsAdmin());
        return user;
    }

    public List<ResponseChannelLogDTO> showChannelLogs(int id) {
        User currentUser = getCurrentUser();
        Channel channel = getById(id);
        if (!channel.getOwner().getUsername().equals(currentUser.getUsername())) {
            throw new ChannelException("Current user is not owner of this channel");
        }
        return channel.getLogs().stream()
                .map(log -> modelMapper.map(log, ResponseChannelLogDTO.class))
                .collect(Collectors.toList());
    }

    public ResponseChannelPostDTO convertToResponseChannelPostsDTO(ChannelPost channelPost) {
        User currentUser = userService.getById(getCurrentUser().getId());
        return ResponseChannelPostDTO.builder()
                .text(channelPost.getText())
                .id(channelPost.getId())
                .owner(modelMapper.map(channelPost.getOwner(), ResponseUserDTO.class))
                .commentsCount(channelPost.getComments() != null && channelPost.getChannel().isPostsCommentsAllowed()
                        ? channelPost.getComments().size() : 0)
                .likes(channelPost.getLikes() != null ? channelPost.getLikes().size() : 0)
                .isLiked(channelPost.getLikes() != null && channelPost.getLikes().contains(currentUser))
                .contentType(channelPost.getContentType())
                .images(channelPost.getFiles() != null ? channelPost.getFiles().stream()
                        .map(image -> new ResponseChannelPostFilesDTO(image.getId()))
                        .toList() : Collections.emptyList())
                .createdAt(channelPost.getCreatedAt())
                .build();
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
            ChannelsOptionsDTO options = new ChannelsOptionsDTO();
            options.setIsFilesAllowed(channel.isFilesAllowed());
            options.setIsPrivate(channel.isPrivate());
            options.setIsPostsCommentsAllowed(channel.isPostsCommentsAllowed());
            options.setIsInvitesAllowed(channel.isInvitesAllowed());
            return options;
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
}