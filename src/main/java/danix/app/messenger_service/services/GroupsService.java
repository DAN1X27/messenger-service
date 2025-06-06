package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.GroupException;
import danix.app.messenger_service.util.FileException;
import danix.app.messenger_service.util.FilesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupsService {
    private final GroupsRepository groupsRepository;
    private final GroupsUsersRepository groupsUsersRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final BlockedUsersRepository blockedUsersRepository;
    private final GroupsInvitesRepository groupsInvitesRepository;
    private final AppMessagesRepository appMessagesRepository;
    private final GroupsActionsMessagesRepository groupsActionsMessagesRepository;
    private final GroupsMessagesRepository messagesRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${default_groups_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${groups_avatars_path}")
    private String AVATARS_PATH;
    @Value("${groups_messages_images_path}")
    private String MESSAGES_IMAGES_PATH;
    @Value("${groups_messages_videos_path}")
    private String MESSAGES_VIDEOS_PATH;
    @Value("${groups_messages_audio_path}")
    private String MESSAGES_AUDIO_PATH;

    public List<ResponseGroupDTO> getAllUserGroups() {
        return groupsUsersRepository.findAllByUser(getCurrentUser()).stream()
                .map(user -> modelMapper.map(user.getGroup(), ResponseGroupDTO.class))
                .collect(Collectors.toList());
    }

    public List<ResponseGroupInviteDTO> getAllUserGroupsInvites() {
        return groupsInvitesRepository.findByUser(getCurrentUser()).stream()
                .map(invite -> modelMapper.map(invite, ResponseGroupInviteDTO.class))
                .collect(Collectors.toList());
    }

    public List<ResponseGroupUserDTO> getGroupUsers(int groupId, int page, int count) {
        Group group = getById(groupId);
        getGroupUser(group, getCurrentUser());
        return groupsUsersRepository.findAllByGroup(group, PageRequest.of(page, count)).stream()
                .map(groupUser -> {
                    ResponseGroupUserDTO responseDTO = modelMapper.map(groupUser.getUser(), ResponseGroupUserDTO.class);
                    responseDTO.setAdmin(groupUser.isAdmin());
                    return responseDTO;
                })
                .toList();
    }

    public ShowGroupDTO showGroup(int groupId) {
        Group group = getById(groupId);
        getGroupUser(group, getCurrentUser());
        return ShowGroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdAt(group.getCreatedAt())
                .owner(modelMapper.map(group.getOwner(), ResponseUserDTO.class))
                .webSocketUUID(group.getWebSocketUUID())
                .usersCount(groupsUsersRepository.countByGroup(group))
                .build();
    }

    @Transactional
    public long createGroup(CreateGroupDTO groupDTO) {
        User currentUser = getCurrentUser();
        Group group = Group.builder()
                .name(groupDTO.getName())
                .description(groupDTO.getDescription())
                .createdAt(new Date())
                .image(DEFAULT_IMAGE_UUID)
                .owner(currentUser)
                .webSocketUUID(UUID.randomUUID().toString())
                .build();
        groupsRepository.save(group);
        GroupUser groupUser = new GroupUser();
        groupUser.setGroup(group);
        groupUser.setUser(currentUser);
        groupUser.setAdmin(true);
        groupsUsersRepository.save(groupUser);
        return group.getId();
    }

    @Transactional
    public void updateGroup(UpdateGroupDTO updateGroupDTO) {
        Group group = getById(updateGroupDTO.getGroupId());
        User currentUser = getCurrentUser();
        if (group.getOwner().getId() == currentUser.getId()) {
            group.setName(updateGroupDTO.getName() != null ? updateGroupDTO.getName() : group.getName());
            group.setDescription(updateGroupDTO.getDescription() != null ? updateGroupDTO.getDescription() : group.getDescription());
            sendUpdatedGroupMessage(group, false);
        } else {
            throw new GroupException("User must be owner of group");
        }
    }

    @Transactional
    public void addImage(MultipartFile image, int id) {
        Group group = getById(id);
        if (group.getOwner().getId() != getCurrentUser().getId()) {
            throw new FileException("User must be owner of group");
        }
        String uuid = UUID.randomUUID().toString();
        FilesUtils.upload(Path.of(AVATARS_PATH), image, uuid, ContentType.IMAGE);
        sendGroupActionMessage(id, getCurrentUser().getUsername() + " updated group image");
        sendUpdatedGroupMessage(group, true);
        if (group.getImage().equals(DEFAULT_IMAGE_UUID)) {
            group.setImage(uuid);
            return;
        }
        FilesUtils.delete(Path.of(AVATARS_PATH), group.getImage());
        group.setImage(uuid);
    }

    @Transactional
    public void deleteImage(int id) {
        Group group = getById(id);
        if (group.getOwner().getId() != getCurrentUser().getId()) {
            throw new FileException("User must be owner of group");
        }
        if (group.getImage().equals(DEFAULT_IMAGE_UUID)) {
            throw new FileException("Group already have default image");
        }
        FilesUtils.delete(Path.of(AVATARS_PATH), group.getImage());
        group.setImage(DEFAULT_IMAGE_UUID);
        sendGroupActionMessage(id, getCurrentUser().getUsername() + " deleted group image");
        sendUpdatedGroupMessage(group, true);
    }

    public ResponseFileDTO getImage(int id) {
        Group group = getById(id);
        getGroupUser(group, getCurrentUser());
        return FilesUtils.download(Path.of(AVATARS_PATH), group.getImage(), ContentType.IMAGE);
    }

    @Transactional
    public void deleteAdmin(int groupId, int userId) {
        Group group = getById(groupId);
        GroupUser groupUser = checkAccessRights(group, userId);
        if (groupUser.isAdmin()) {
            groupUser.setAdmin(false);
        } else {
            throw new GroupException("User is not admin");
        }
    }

    @Transactional
    public void addAdmin(int groupId, int userId) {
        Group group = getById(groupId);
        GroupUser groupUser = checkAccessRights(group, userId);
        if (!groupUser.isAdmin()) {
            groupUser.setAdmin(true);
        } else {
            throw new GroupException("User is already admin");
        }
    }

    private GroupUser checkAccessRights(Group group, int userId) {
        User currentUser = getCurrentUser();
        getGroupUser(group, currentUser);
        User user = userService.getById(userId);
        if (group.getOwner().getId() != currentUser.getId()) {
            throw new GroupException("Current user must be owner of group");
        }
        try {
            return getGroupUser(group, user);
        } catch (GroupException e) {
            throw new GroupException("User not exist in this group");
        }
    }

    @Transactional
    public long inviteUser(int groupId, int userId) {
        User user = userService.getById(userId);
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        getGroupUser(group, currentUser);
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresent(blockedUser -> {
            throw new GroupException("User has blocked you");
        });
        blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(blockedUser -> {
            throw new GroupException("User has blocked by current user");
        });
        if (user.getIsPrivate()) {
            if (userService.findUserFriend(user, currentUser) == null) {
                throw new GroupException("User has a private account");
            }
        }
        groupsUsersRepository.findByGroupAndUser(group, user).ifPresent(groupUser -> {
            throw new GroupException("User already exist in this group");
        });
        if (group.getBannedUsers().contains(user)) {
            throw new GroupException("User has been banned in this group");
        }
        groupsInvitesRepository.findByGroupAndUser(group, user).ifPresent(groupsInvitesUser -> {
            throw new GroupException("User already have invite");
        });
        GroupInvite groupInvite = new GroupInvite();
        groupInvite.setGroup(group);
        groupInvite.setUser(user);
        groupsInvitesRepository.save(groupInvite);
        return groupInvite.getId();
    }

    @Transactional
    public void acceptInviteToGroup(int groupId) {
        User currentUser = userService.getById(getCurrentUser().getId());
        Group group = getById(groupId);
        groupsInvitesRepository.findByGroupAndUser(group, currentUser).ifPresentOrElse(invite -> {
            groupsUsersRepository.findByGroupAndUser(group, currentUser).ifPresent(groupUser -> {
                throw new GroupException("Current user already exist in this group");
            });
            if (group.getBannedUsers().contains(currentUser)) {
                throw new GroupException("Current user has been banned in this group");
            }
            GroupUser groupUser = new GroupUser();
            groupUser.setGroup(group);
            groupUser.setUser(currentUser);
            groupUser.setAdmin(false);
            groupsUsersRepository.save(groupUser);
            groupsInvitesRepository.delete(invite);
            sendGroupActionMessage(groupId, currentUser.getUsername() + " joined to group");
        }, () -> {
            throw new GroupException("Invite not found");
        });
    }

    @Transactional
    public CompletableFuture<Void> deleteGroup(int groupId) {
        Group group = getById(groupId);
        if (group.getOwner().getId() == getCurrentUser().getId()) {
            if (group.getImage() != null && !group.getImage().equals(DEFAULT_IMAGE_UUID)) {
                FilesUtils.delete(Path.of(AVATARS_PATH), group.getImage());
            }
            return CompletableFuture.runAsync(() -> {
                List<GroupMessage> messages;
                int page = 0;
                do {
                    messages = messagesRepository.findAllByGroupAndContentTypeIsNot(group, ContentType.TEXT,
                            PageRequest.of(page, 50));
                    for (GroupMessage message : messages) {
                        switch (message.getContentType()) {
                            case IMAGE -> FilesUtils.delete(Path.of(MESSAGES_IMAGES_PATH), message.getText());
                            case VIDEO -> FilesUtils.delete(Path.of(MESSAGES_VIDEOS_PATH), message.getText());
                            case AUDIO_MP3, AUDIO_OGG -> FilesUtils.delete(Path.of(MESSAGES_AUDIO_PATH), message.getText());
                        }
                    }
                    page++;
                } while (!messages.isEmpty());
                groupsRepository.deleteById(groupId);
                messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(),
                        new ResponseDeletionGroupDTO(groupId));
            });
        } else {
            throw new GroupException("You are not owner of this group");
        }
    }

    @Transactional
    public CompletableFuture<Void> leaveGroup(int groupId) {
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        GroupUser groupUser = getGroupUser(group, currentUser);
        if (group.getOwner().getId() == currentUser.getId()) {
            return deleteGroup(groupId);
        } else {
            groupsUsersRepository.delete(groupUser);
            sendGroupActionMessage(groupId, currentUser.getUsername() + " left group");
        }
        return null;
    }

    @Transactional
    public void banUser(int groupId, int userId) {
        User user = userService.getById(userId);
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        GroupUser groupUser = getGroupUser(group, currentUser);
        if (groupUser.isAdmin()) {
            if (group.getBannedUsers().contains(user)) {
                throw new GroupException("User already banned in this group");
            }
            groupsUsersRepository.findByGroupAndUser(group, user).ifPresent(foundUser -> {
                if (foundUser.isAdmin() && group.getOwner().getId() != currentUser.getId()) {
                    throw new GroupException("Current user cant ban administrator or owner of group");
                }
                groupsUsersRepository.delete(foundUser);
                AppMessage appMessage = new AppMessage("You were banned from the group - " + group.getName(), user);
                appMessagesRepository.save(appMessage);
                messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(),
                        Map.of("deleted_user_id", userId));
                messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/main",
                        new ResponseAppMessageDTO(appMessage.getMessage(), appMessage.getSentTime()));
                messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/main",
                        new ResponseDeletionGroupDTO(groupId));
                sendGroupActionMessage(groupId, currentUser.getUsername() + " banned " + user.getUsername());
            });
            group.getBannedUsers().add(user);
        } else {
            throw new GroupException("Current must be admin of this group");
        }
    }

    @Transactional
    public void unbanUser(int groupId, int userId) {
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        GroupUser groupUser = getGroupUser(group, currentUser);
        if (groupUser.isAdmin()) {
            User user = userService.getById(userId);
            if (group.getBannedUsers().contains(user)) {
                group.getBannedUsers().remove(user);
            } else {
                throw new GroupException("User is not banned in this group");
            }
        } else {
            throw new GroupException("Current must be admin of this group");
        }
    }

    @Transactional
    public void kickUser(int groupId, int userId) {
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        GroupUser groupUser = getGroupUser(group, currentUser);
        if (groupUser.isAdmin()) {
            User user = userService.getById(userId);
            GroupUser kickedUser;
            try {
                kickedUser = getGroupUser(group, user);
            } catch (GroupException e) {
                throw new GroupException("User not exists in this group");
            }
            if (kickedUser.isAdmin() && group.getOwner().getId() != currentUser.getId()) {
                throw new GroupException("Current user cant kick administrator or owner of group");
            }
            groupsUsersRepository.delete(kickedUser);
            sendGroupActionMessage(groupId, currentUser.getUsername() + " kicked " + user.getUsername());
            messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/main",
                    new ResponseDeletionGroupDTO(groupId));
            AppMessage appMessage = new AppMessage("You were kicked from the group - " + group.getName(), user);
            appMessagesRepository.save(appMessage);
            messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/main",
                    new ResponseAppMessageDTO(appMessage.getMessage(), appMessage.getSentTime()));
            messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(),
                    Map.of("deleted_user_id", userId));
        } else {
            throw new GroupException("Current must be admin of this group");
        }
    }

    private void sendGroupActionMessage(int groupId, String message) {
        Group group = getById(groupId);
        GroupActionMessage groupActionMessage = new GroupActionMessage(message, group);
        groupsActionsMessagesRepository.save(groupActionMessage);
        messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(),
                ResponseGroupActionMessageDTO.builder()
                        .text(message)
                        .sentTime(LocalDateTime.now())
                        .build());
    }

    private void sendUpdatedGroupMessage(Group group, boolean imageUpdated) {
        ResponseGroupUpdatingDTO response =
                new ResponseGroupUpdatingDTO(modelMapper.map(group, ResponseGroupDTO.class), imageUpdated);
        messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(), response);
    }

    public GroupUser getGroupUser(Group group, User user) {
        return groupsUsersRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new GroupException("Current user not exist in this group"));
    }

    public Group getById(int id) {
        return groupsRepository.findById(id)
                .orElseThrow(() -> new GroupException("Group not found"));
    }
}