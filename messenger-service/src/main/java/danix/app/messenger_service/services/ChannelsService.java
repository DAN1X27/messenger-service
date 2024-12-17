package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.ChannelException;
import danix.app.messenger_service.util.ImageException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
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
public class ChannelsService implements Image {
    private final ChannelsRepository channelsRepository;
    private final ChannelsUsersRepository channelsUsersRepository;
    private final ChannelsInvitesRepository channelsInvitesRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final ChannelsLogsRepository channelsLogsRepository;
    private final AppMessagesRepository appMessagesRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BlockedUsersRepository blockedUsersRepository;
    private final BannedChannelsUsersRepository bannedUsersRepository;
    private final ChannelsPostsLikesRepository channelsPostsLikesRepository;
    private final ChannelsPostsRepository channelsPostsRepository;


    @Value("${default_channels_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${channels_avatars_path}")
    private String AVATARS_PATH;

    public List<ResponseChannelDTO> getAllUserChannels() {
        return channelsUsersRepository.findAllByUser(getCurrentUser()).stream()
                .filter(user -> !user.getChannel().isBaned())
                .map(user -> modelMapper.map(user.getChannel(), ResponseChannelDTO.class))
                .collect(Collectors.toList());
    }

    public ResponseChannelDTO findChannel(String name) {
        Channel channel = channelsRepository.findByNameStartsWith(name)
                .orElseThrow(() -> new ChannelException("Channel not found"));
        if (!channel.getIsPrivate() && !channel.isBaned() || getCurrentUser().getRole() == User.Roles.ROLE_ADMIN) {
            return modelMapper.map(channel, ResponseChannelDTO.class);
        }
        throw new ChannelException("Channel not found");
    }

    List<ResponseChannelInviteDTO> getChannelsInvites() {
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
                    .isBanned(false)
                    .owner(getCurrentUser())
                    .image(DEFAULT_IMAGE_UUID)
                    .isPrivate(createChannelDTO.getIsPrivate() != null && createChannelDTO.getIsPrivate())
                    .build();

            ChannelUser channelUser = new ChannelUser();
            channelUser.setChannel(channel);
            channelUser.setUser(getCurrentUser());
            channelUser.setIsAdmin(true);
            channelsUsersRepository.save(channelUser);
            channelsRepository.save(channel);
        });
    }

    @Transactional
    public void addImage(MultipartFile image, int channelId) {
        Channel channel = getById(channelId);
        if (channel.getOwner().getId() != getCurrentUser().getId()) {
            throw new ImageException("Current user must be owner of channel");
        }
        String uuid = UUID.randomUUID().toString();
        ImageService.upload(Path.of(AVATARS_PATH), image, uuid);
        if (channel.getImage().equals(DEFAULT_IMAGE_UUID)) {
            channel.setImage(uuid);
            return;
        }
        ImageService.delete(Path.of(AVATARS_PATH), channel.getImage());
        channel.setImage(uuid);
    }

    public ResponseImageDTO getImage(int channelId) {
        Channel channel = getById(channelId);
        getChannelUser(getCurrentUser(), channel);
        return ImageService.download(Path.of(AVATARS_PATH), channel.getImage());
    }

    @Transactional
    public void deleteImage(int channelId) {
        Channel channel = getById(channelId);
        if (channel.getOwner().getId() != getCurrentUser().getId()) {
            throw new ImageException("Current user must be owner of channel");
        } else if (channel.getImage().equals(DEFAULT_IMAGE_UUID)) {
            throw new ImageException("Channel already have default image");
        }
        ImageService.delete(Path.of(AVATARS_PATH), channel.getImage());
        channel.setImage(DEFAULT_IMAGE_UUID);
    }

    @Transactional
    public void inviteToChannel(int channelId, int userId) {
        Channel channel = getById(channelId);
        User user = userService.getById(userId);
        getChannelUser(getCurrentUser(), channel);
        blockedUsersRepository.findByOwnerAndBlockedUser(user, getCurrentUser()).ifPresent(blockedUser -> {
            throw new ChannelException("Current user are blocked from this user");
        });
        channelsInvitesRepository.findByUserAndChannel(user, channel).ifPresent(invite -> {
            throw new ChannelException("User already have invite to this channel");
        });
        if (channelsUsersRepository.findByUserAndChannel(user, channel).isPresent()) {
            throw new ChannelException("User already exist in this channel");
        } else if (bannedUsersRepository.findByUserAndChannel(user, channel).isPresent()) {
            throw new ChannelException("User is banned in this channel");
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
        ChannelUser channelUser = channelsUsersRepository.findByUserAndChannel(getCurrentUser(), channel)
                .orElseThrow(() -> new ChannelException("Current user not exist in this channel"));
        channelsUsersRepository.delete(channelUser);
        if (channel.getOwner().getUsername().equals(getCurrentUser().getUsername())) {
            channelsRepository.delete(channel);
        }
    }

    @Transactional
    public void banChannel(BanChannelDTO banChannelDTO) {
        Channel channel = getById(banChannelDTO.getChannelId());
        if (channel.isBaned()) throw new ChannelException("Channel is already banned");
        channel.setBaned(true);
        kafkaTemplate.send("ban_channel-topic", channel.getOwner().getEmail(),
                "Your channel with name " + channel.getName() + " has been banned for reason: " + banChannelDTO.getReason());
        AppMessage appMessage = new AppMessage(
                "Your channel with name " + channel.getName() + " has been banned for reason: " + banChannelDTO.getReason(),
                channel.getOwner()
        );
        appMessagesRepository.save(appMessage);
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
    }

    @Transactional
    public void acceptInviteToChannel(int id) {
        Channel channel = getById(id);
        ChannelInvite invite = channelsInvitesRepository.findByUserAndChannel(getCurrentUser(), channel)
                .orElseThrow(() -> new ChannelException("Invite not found"));
        if (bannedUsersRepository.findByUserAndChannel(getCurrentUser(), channel).isEmpty()) {
            bannedUsersRepository.findByUserAndChannel(getCurrentUser(), channel).ifPresent(bannedUser -> {
                throw new ChannelException("Current user banned in this channel");
            });
            ChannelUser channelUser = new ChannelUser();
            channelUser.setChannel(channel);
            channelUser.setUser(getCurrentUser());
            channelUser.setIsAdmin(false);
            channelsUsersRepository.save(channelUser);
            channelsInvitesRepository.delete(invite);
        } else {
            throw new ChannelException("Current user already exist in this channel");
        }
    }

    @Transactional
    public void banUser(int channelId, int userId) {
        Channel channel = getById(channelId);
        User currentUser = getCurrentUser();
        ChannelUser channelUser = channelsUsersRepository.findByUserAndChannel(currentUser, channel)
                .orElseThrow(() -> new ChannelException("Current user not exist in this channel"));
        User user = userService.getById(userId);
        if (!channelUser.getIsAdmin()) {
            throw new ChannelException("Current user is not admin");
        }
        bannedUsersRepository.findByUserAndChannel(user, channel).ifPresent(bannedUser -> {
            throw new ChannelException("User already banned");
        });
        channelsUsersRepository.findByUserAndChannel(user, channel).ifPresent(channelUser1 -> {
            if (channelUser1.getIsAdmin() && !channel.getOwner().getUsername().equals(getCurrentUser().getUsername())) {
                throw new ChannelException("Current user can't ban admin");
            }
            channelsUsersRepository.delete(channelUser1);
            AppMessage appMessage = new AppMessage("You were banned in channel - " + channel.getName(), user);
            appMessagesRepository.save(appMessage);
        });
        BannedChannelUser bannedChannelUser = new BannedChannelUser();
        bannedChannelUser.setChannel(channel);
        bannedChannelUser.setUser(user);
        BannedChannelUserKey bannedChannelUserKey = new BannedChannelUserKey();
        bannedChannelUserKey.setChannelId(channel.getId());
        bannedChannelUserKey.setUserId(user.getId());
        bannedChannelUser.setId(bannedChannelUserKey);
        bannedUsersRepository.save(bannedChannelUser);
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
        bannedUsersRepository.findByUserAndChannel(user, channel).ifPresentOrElse(bannedUser -> {
            bannedUsersRepository.delete(bannedUser);
            ChannelLog channelLog = new ChannelLog();
            channelLog.setChannel(channel);
            channelLog.setMessage(currentUser.getUsername() + " unbanned user - " + user.getUsername());
            channelsLogsRepository.save(channelLog);
        }, () -> {
            throw new ChannelException("User is not banned");
        });
    }

    @Transactional
    public void addAdmin(int channelId, int userId) {
        Channel channel = getById(channelId);
        User currentUser = getCurrentUser();
        if (channel.getOwner().getUsername().equals(currentUser.getUsername())) {
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
        if (channel.getOwner().getUsername().equals(currentUser.getUsername())) {
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
        if (channel.getOwner().getUsername().equals(getCurrentUser().getUsername())) {
            channel.setName(updateChannelDTO.getName() == null ? channel.getName() : updateChannelDTO.getName());
            channel.setDescription(updateChannelDTO.getDescription() == null ? channel.getDescription() : updateChannelDTO.getDescription());
            channel.setIsPrivate(updateChannelDTO.getIsPrivate() == null ? channel.getIsPrivate() : updateChannelDTO.getIsPrivate());
        } else {
            throw new ChannelException("Current user is not owner of this channel");
        }
    }

    @Transactional
    public void deleteChannel(int id) {
        Channel channel = getById(id);
        if (channel.getOwner().getUsername().equals(getCurrentUser().getUsername())) {
            channelsRepository.delete(channel);
        } else {
            throw new ChannelException("Current user is not owner of this channel");
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
                .isPrivate(channel.getIsPrivate())
                .createdAt(channel.getCreatedAt())
                .posts(channelsPostsRepository.findAllByChannel(channel, PageRequest.of(postsPage, postsCount)).stream()
                        .map(this::convertToResponseChannelPostsDTO)
                        .toList())
                .users(channel.getUsers().stream()
                        .map(this::convertToResponseChannelUserDTO)
                        .toList())
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

    private ResponseChannelPostDTO convertToResponseChannelPostsDTO(ChannelPost channelPost) {
        ResponseChannelPostDTO responseChannelPostDTO = ResponseChannelPostDTO.builder()
                .text(channelPost.getPost())
                .id(channelPost.getId())
                .owner(channelPost.getOwner().getUsername())
                .commentsCount(channelPost.getComments().size())
                .likes(channelPost.getLikes().size())
                .contentType(channelPost.getContentType())
                .images(channelPost.getImages().stream()
                        .map(image -> new ResponseChannelPostImageDTO(image.getId()))
                        .toList())
                .build();
        channelsPostsLikesRepository.findByUserAndPost(getCurrentUser(), channelPost)
                .ifPresentOrElse(like -> responseChannelPostDTO.setLiked(true), () -> responseChannelPostDTO.setLiked(false));
        return responseChannelPostDTO;
    }

    @Transactional
    public void joinToChannel(int id) {
        User currentUser = getCurrentUser();
        Channel channel = getById(id);
        channelsUsersRepository.findByUserAndChannel(currentUser, channel).ifPresentOrElse(user -> {
            throw new ChannelException("Current user already exist in this channel");
        }, () -> {
            if (channel.getIsPrivate()) {
                throw new ChannelException("Channel is private");
            }
            bannedUsersRepository.findByUserAndChannel(currentUser, channel).ifPresent(user -> {
                throw new ChannelException("Current user banned in this channel");
            });
        });
        ChannelUser channelUser = new ChannelUser();
        channelUser.setChannel(channel);
        channelUser.setUser(currentUser);
        channelUser.setIsAdmin(false);
        channelsUsersRepository.save(channelUser);
    }

    Channel getById(int id) {
        Channel channel = channelsRepository.findById(id)
                .orElseThrow(() -> new ChannelException("Channel not found"));
        if (channel.isBaned() && getCurrentUser().getRole() != User.Roles.ROLE_ADMIN) {
            throw new ChannelException("Channel not found");
        }
        return channel;
    }

    ChannelUser getChannelUser(User user, Channel channel) {
        return channelsUsersRepository.findByUserAndChannel(user, channel)
                .orElseThrow(() -> new ChannelException("Current user not exist in this channel"));
    }
}