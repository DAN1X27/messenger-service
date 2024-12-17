package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.ResponseImageDTO;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.GroupsMessagesRepository;
import danix.app.messenger_service.util.ImageException;
import danix.app.messenger_service.util.MessageException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Service
@RequiredArgsConstructor
public class GroupsMessagesService {
    private final GroupsMessagesRepository groupsMessagesRepository;
    private final GroupsService groupsService;

    @Value("${groups_images_path}")
    private String GROUPS_IMAGES_PATH;

    @Transactional
    public void sendImage(MultipartFile image, int groupId) {
        String uuid = UUID.randomUUID().toString();
        sendMessage(uuid, groupId, ContentType.IMAGE);
        Path path = Path.of(GROUPS_IMAGES_PATH);
        ImageService.upload(path, image, uuid);
    }

    @Transactional
    public void sendTextMessage(String message, int groupId) {
        sendMessage(message, groupId, ContentType.TEXT);
    }

    public ResponseImageDTO getImage(long messageId) {
        GroupMessage groupMessage = groupsMessagesRepository.findById(messageId)
                .orElseThrow(() -> new ImageException("Image not found"));
        Group group = groupMessage.getGroup();
        groupsService.getGroupUser(group, getCurrentUser());
        return ImageService.download(Path.of(GROUPS_IMAGES_PATH), groupMessage.getMessage());
    }

    @Transactional
    public void sendMessage(String message, int groupId, ContentType contentType) {
        User currentUser = getCurrentUser();
        Group group = groupsService.getById(groupId);
        groupsService.getGroupUser(group, currentUser);
        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setMessage(message);
        groupMessage.setGroup(group);
        groupMessage.setContentType(contentType);
        groupMessage.setSentTime(LocalDateTime.now());
        groupMessage.setMessageOwner(currentUser);
        groupsMessagesRepository.save(groupMessage);
    }

    @Transactional
    public void deleteMessage(long messageId) {
        User currentUser = getCurrentUser();
        GroupMessage message = groupsMessagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Group group = message.getGroup();
        GroupUser groupUser = groupsService.getGroupUser(group, currentUser);
        if (message.getMessageOwner().getUsername().equals(currentUser.getUsername()) || groupUser.isAdmin()) {
            if (message.getContentType() == ContentType.IMAGE) {
                ImageService.delete(Path.of(GROUPS_IMAGES_PATH), message.getMessage());
            }
            groupsMessagesRepository.delete(message);
        } else {
            throw new MessageException("User must be owner of this message or admin");
        }
    }

    @Transactional
    public void updateMessage(long messageId, String text) {
        User currentUser = getCurrentUser();
        GroupMessage message = groupsMessagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        if (message.getContentType() == ContentType.IMAGE) {
            throw new MessageException("The image cannot be changed");
        }
        Group group = message.getGroup();
        groupsService.getGroupUser(group, currentUser);
        if (message.getMessageOwner().getUsername().equals(currentUser.getUsername())) {
            message.setMessage(text);
        } else {
            throw new MessageException("User must be owner of this message");
        }
    }
}
