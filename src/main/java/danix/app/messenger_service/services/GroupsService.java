package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.GroupException;
import danix.app.messenger_service.util.ImageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupsService implements Image {
    private final GroupsRepository groupsRepository;
    private final GroupsUsersRepository groupsUsersRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final BlockedUsersRepository blockedUsersRepository;
    private final GroupsInvitesRepository groupsInvitesRepository;
    private final AppMessagesRepository appMessagesRepository;
    private final GroupsActionsMessagesRepository groupsActionsMessagesRepository;
    private final GroupsBannedUsersRepository groupsBannedUsersRepository;
    private final GroupsMessagesRepository messagesRepository;

    @Value("${default_groups_image_uuid}")
    private String DEFAULT_IMAGE_UUID;
    @Value("${groups_avatars_path}")
    private String DEFAULT_IMAGES_PATH;

    public List<ResponseGroupDTO> getAllUserGroups() {
        return groupsUsersRepository.findAllByUser(getCurrentUser()).stream()
                .map(user -> modelMapper.map(user.getGroup(), ResponseGroupDTO.class))
                .collect(Collectors.toList());
    }

    public List<GroupInvite> getAllUserGroupsInvites() {
        return groupsInvitesRepository.findByUser(getCurrentUser());
    }

    public ShowGroupDTO showGroup(int groupId, int page, int count) {
        Group group = getById(groupId);
        getGroupUser(group, getCurrentUser());
        return ShowGroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdAt(group.getCreatedAt())
                .messages(messagesRepository.findAllByGroup(group, PageRequest.of(page, count)).stream()
                        .map(this::convertToMessageDTO)
                        .toList())
                .users(group.getUsers().stream()
                        .map(this::convertToUserDTO)
                        .toList())
                .groupActionMessages(group.getActionMessages().stream()
                        .map(message -> modelMapper.map(message, ResponseGroupActionMessageDTO.class))
                        .toList())
                .usersCount(group.getUsers().size())
                .owner(modelMapper.map(group.getOwner(), ResponseUserDTO.class))
                .build();
    }

    private ResponseGroupUserDTO convertToUserDTO(GroupUser user) {
        ResponseGroupUserDTO respUser = new ResponseGroupUserDTO();
        respUser.setId(user.getUser().getId());
        respUser.setUsername(user.getUser().getUsername());
        respUser.setAdmin(user.isAdmin());
        return respUser;
    }

    private ResponseGroupMessageDTO convertToMessageDTO(GroupMessage message) {
        ResponseGroupMessageDTO messageDTO = modelMapper.map(message, ResponseGroupMessageDTO.class);
        messageDTO.setMessage(message.getContentType() == ContentType.TEXT ? message.getMessage() : null);
        messageDTO.setMessageId(message.getId());
        messageDTO.setSender(modelMapper.map(message.getMessageOwner(), ResponseUserDTO.class));
        return messageDTO;
    }

    @Transactional
    public void createGroup(CreateGroupDTO groupDTO) {
        User currentUser = getCurrentUser();
        Group group = Group.builder()
                .name(groupDTO.getName())
                .description(groupDTO.getDescription())
                .createdAt(new Date())
                .image(DEFAULT_IMAGE_UUID)
                .owner(currentUser)
                .build();

        GroupUser groupUser = new GroupUser();
        groupUser.setGroup(group);
        groupUser.setUser(currentUser);
        groupUser.setAdmin(true);
        groupsUsersRepository.save(groupUser);
    }

    @Transactional
    public void updateGroup(UpdateGroupDTO updateGroupDTO) {
        Group group = getById(updateGroupDTO.getGroupId());
        if (group.getOwner().getUsername().equals(getCurrentUser().getUsername())) {
            group.setName(updateGroupDTO.getName() != null ? updateGroupDTO.getName() : group.getName());
            group.setDescription(updateGroupDTO.getDescription() != null ? updateGroupDTO.getDescription() : group.getDescription());
        } else {
            throw new GroupException("User must be owner of group");
        }
    }

    @Transactional
    public void addImage(MultipartFile image, int id) {
        Group group = getById(id);
        if (group.getOwner().getId() != getCurrentUser().getId()) {
            throw new ImageException("User must be owner of group");
        }
        String uuid = UUID.randomUUID().toString();
        ImageService.upload(Path.of(DEFAULT_IMAGES_PATH), image, uuid);
        if (group.getImage().equals(DEFAULT_IMAGE_UUID)) {
            group.setImage(uuid);
            return;
        }
        ImageService.delete(Path.of(DEFAULT_IMAGES_PATH), group.getImage());
        group.setImage(uuid);
    }

    @Override
    @Transactional
    public void deleteImage(int id) {
        Group group = getById(id);
        if (group.getOwner().getId() != getCurrentUser().getId()) {
            throw new ImageException("User must be owner of group");
        }else if (group.getImage().equals(DEFAULT_IMAGE_UUID)) {
            throw new ImageException("Group already have default image");
        }
        ImageService.delete(Path.of(DEFAULT_IMAGES_PATH), group.getImage());
        group.setImage(DEFAULT_IMAGE_UUID);
    }

    @Override
    public ResponseImageDTO getImage(int id) {
        Group group = getById(id);
        getGroupUser(group, getCurrentUser());
        return ImageService.download(Path.of(DEFAULT_IMAGES_PATH), group.getImage());
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
        GroupUser groupUser;
        try {
            groupUser = getGroupUser(group, user);
        } catch (GroupException e) {
            throw new GroupException("User not exist in this group");
        }
        if (!group.getOwner().getUsername().equals(currentUser.getUsername())) {
            throw new GroupException("Current user must be owner of group");
        }
        return groupUser;
    }

    @Transactional
    public void inviteUser(int groupId, int userId) {
        User user = userService.getById(userId);
        User currentUser = getCurrentUser();
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresent(blockedUser -> {
            throw new GroupException("User has blocked you");
        });
        Group group = getById(groupId);
        getGroupUser(group, currentUser);
        groupsUsersRepository.findByGroupAndUser(group, user).ifPresentOrElse(groupUser -> {
            throw new GroupException("User already exist in this group");
        }, () -> {
            groupsBannedUsersRepository.findByGroupAndUser(group, user).ifPresentOrElse(bannedUser -> {
                throw new GroupException("User is banned in this group");
            }, () -> {
                groupsInvitesRepository.findByGroupAndUser(group, user).ifPresentOrElse(invite -> {
                    throw new GroupException("User already have invite");
                }, () -> {
                    GroupInvite groupInvite = new GroupInvite();
                    groupInvite.setGroup(group);
                    groupInvite.setUser(user);
                    groupsInvitesRepository.save(groupInvite);
                });
            });
        });
    }

    @Transactional
    public void acceptInviteToGroup(int groupId) {
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        groupsInvitesRepository.findByGroupAndUser(group, currentUser).ifPresentOrElse(invite -> {
            groupsUsersRepository.findByGroupAndUser(group, currentUser).ifPresent(groupUser -> {
                throw new GroupException("Current user already exist in this group");
            });
            groupsBannedUsersRepository.findByGroupAndUser(group, currentUser).ifPresent(bannedUser -> {
                throw new GroupException("Current user banned in this group");
            });
            GroupUser groupUser = new GroupUser();
            groupUser.setGroup(group);
            groupUser.setUser(currentUser);
            groupUser.setAdmin(false);
            groupsUsersRepository.save(groupUser);
            groupsInvitesRepository.delete(invite);
            GroupActionMessage groupActionMessage = new GroupActionMessage();
            groupActionMessage.setMessage(currentUser.getUsername() + " joined to group");
            groupActionMessage.setGroup(group);
            groupsActionsMessagesRepository.save(groupActionMessage);
        }, () -> {
            throw new GroupException("Invite not found");
        });
    }

    @Transactional
    public void deleteGroup(int groupId) {
        Group group = getById(groupId);
        if (group.getOwner().getId() == getCurrentUser().getId()) {
            groupsRepository.delete(group);
        } else {
            throw new GroupException("You are not owner of this group");
        }
    }

    @Transactional
    public void leaveGroup(int groupId) {
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        GroupUser groupUser = getGroupUser(group, currentUser);
        if (group.getOwner().getId() == getCurrentUser().getId()) {
            groupsRepository.delete(group);
        } else {
            groupsUsersRepository.delete(groupUser);
            GroupActionMessage groupActionMessage = new GroupActionMessage();
            groupActionMessage.setMessage(currentUser.getUsername() + " left group");
            groupActionMessage.setGroup(group);
            groupsActionsMessagesRepository.save(groupActionMessage);
        }
    }

    @Transactional
    public void banUser(int groupId, int userId) {
        User user = userService.getById(userId);
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        GroupUser groupUser = getGroupUser(group, currentUser);
        if (groupUser.isAdmin()) {
            groupsBannedUsersRepository.findByGroupAndUser(group, user).ifPresent(bannedUser -> {
                throw new GroupException("User is already banned in this group");
            });
            groupsUsersRepository.findByGroupAndUser(group, user).ifPresent(user1 -> {
                if (user1.isAdmin() && group.getOwner().getId() != currentUser.getId()) {
                    throw new GroupException("Current user cant ban administrator or owner of group");
                }
                groupsUsersRepository.delete(user1);
                AppMessage appMessage = new AppMessage("You were banned from the group - " + group.getName(), user);
                appMessagesRepository.save(appMessage);
            });
            GroupBannedUser groupBannedUser = new GroupBannedUser();
            groupBannedUser.setGroup(group);
            groupBannedUser.setUser(user);
            BannedGroupUserKey bannedGroupUserKey = new BannedGroupUserKey();
            bannedGroupUserKey.setUserId(user.getId());
            bannedGroupUserKey.setGroupId(group.getId());
            groupBannedUser.setId(bannedGroupUserKey);
            groupsBannedUsersRepository.save(groupBannedUser);
        } else {
            throw new GroupException("Current user must be owner or admin");
        }
    }

    @Transactional
    public void unbanUser(int groupId, int userId) {
        User currentUser = getCurrentUser();
        Group group = getById(groupId);
        GroupUser groupUser = getGroupUser(group, currentUser);
        if (groupUser.isAdmin()) {
            User user = userService.getById(userId);
            groupsBannedUsersRepository.findByGroupAndUser(group, user).ifPresentOrElse(
                    groupsBannedUsersRepository::delete, () -> {
                        throw new GroupException("User is not banned");
                    }
            );
        } else {
            throw new GroupException("User must be owner or admin");
        }
    }

    GroupUser getGroupUser(Group group, User user) {
        return groupsUsersRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new GroupException("Current user not exist in this group"));
    }

    Group getById(int id) {
        return groupsRepository.findById(id)
                .orElseThrow(() -> new GroupException("Group not found"));
    }
}