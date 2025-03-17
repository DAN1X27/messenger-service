package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.GroupsMessagesRepository;
import danix.app.messenger_service.util.FileException;
import danix.app.messenger_service.util.FileUtils;
import danix.app.messenger_service.util.GroupException;
import danix.app.messenger_service.util.MessageException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Service
@RequiredArgsConstructor
public class GroupsMessagesService {
    private final GroupsMessagesRepository groupsMessagesRepository;
    private final GroupsService groupsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;

    @Value("${groups_messages_images_path}")
    private String GROUPS_MESSAGES_IMAGES_PATH;
    @Value("${groups_messages_videos_path}")
    private String GROUPS_MESSAGES_VIDEOS_PATH;
    @Value("${groups_messages_audio_path}")
    private String GROUPS_MESSAGES_AUDIO_PATH;

    @Transactional
    public void sendFile(MultipartFile image, int groupId, ContentType contentType) {
        if (contentType != ContentType.IMAGE && contentType != ContentType.VIDEO &&
            contentType != ContentType.AUDIO_MP3 && contentType != ContentType.AUDIO_OGG) {
            throw new MessageException("Unsupported content type");
        }
        String uuid = UUID.randomUUID().toString();
        switch (contentType) {
            case IMAGE -> FileUtils.upload(Path.of(GROUPS_MESSAGES_IMAGES_PATH), image, uuid, contentType);
            case VIDEO -> FileUtils.upload(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), image, uuid, contentType);
            case AUDIO_MP3, AUDIO_OGG -> FileUtils.upload(Path.of(GROUPS_MESSAGES_AUDIO_PATH), image, uuid, contentType);
        }
        try {
            sendMessage(uuid, groupId, contentType);
        } catch (GroupException e) {
            FileUtils.delete(Path.of(GROUPS_MESSAGES_IMAGES_PATH), uuid);
            FileUtils.delete(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), uuid);
            throw e;
        }
    }

    @Transactional
    public void sendTextMessage(String message, int groupId) {
        sendMessage(message, groupId, ContentType.TEXT);
    }

    public ResponseFileDTO getFile(long messageId) {
        GroupMessage groupMessage = groupsMessagesRepository.findById(messageId)
                .orElseThrow(() -> new FileException("Image not found"));
        Group group = groupMessage.getGroup();
        groupsService.getGroupUser(group, getCurrentUser());
        switch (groupMessage.getContentType()) {
            case IMAGE -> {
                return FileUtils.download(Path.of(GROUPS_MESSAGES_IMAGES_PATH), groupMessage.getText(), groupMessage.getContentType());
            }
            case VIDEO -> {
                return FileUtils.download(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), groupMessage.getText(), groupMessage.getContentType());
            }
            case AUDIO_OGG, AUDIO_MP3 -> {
                return FileUtils.download(Path.of(GROUPS_MESSAGES_AUDIO_PATH), groupMessage.getText(), groupMessage.getContentType());
            }
            default -> throw new MessageException("Message is not file");
        }
    }

    private void sendMessage(String message, int groupId, ContentType contentType) {
        User currentUser = getCurrentUser();
        Group group = groupsService.getById(groupId);
        groupsService.getGroupUser(group, currentUser);
        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setText(message);
        groupMessage.setGroup(group);
        groupMessage.setContentType(contentType);
        groupMessage.setSentTime(LocalDateTime.now());
        groupMessage.setMessageOwner(currentUser);
        groupsMessagesRepository.save(groupMessage);
        ResponseGroupMessageDTO messageDTO = new ResponseGroupMessageDTO();
        messageDTO.setMessage(message);
        messageDTO.setMessageId(groupMessage.getId());
        messageDTO.setSentTime(groupMessage.getSentTime());
        messageDTO.setSender(modelMapper.map(groupMessage.getMessageOwner(), ResponseUserDTO.class));
        messageDTO.setContentType(contentType);
        messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(), messageDTO);
    }

    @Transactional
    public void deleteMessage(long messageId) {
        User currentUser = getCurrentUser();
        GroupMessage message = groupsMessagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Group group = message.getGroup();
        GroupUser groupUser = groupsService.getGroupUser(group, currentUser);
        if (message.getMessageOwner().getId() == currentUser.getId() || groupUser.isAdmin()) {
            switch (message.getContentType()) {
                case IMAGE -> FileUtils.delete(Path.of(GROUPS_MESSAGES_IMAGES_PATH), message.getText());
                case VIDEO -> FileUtils.delete(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), message.getText());
                case AUDIO_MP3, AUDIO_OGG -> FileUtils.delete(Path.of(GROUPS_MESSAGES_AUDIO_PATH), message.getText());
            }
            groupsMessagesRepository.delete(message);
            messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(),
                    Map.of("deleted_message_id", messageId));
        } else {
            throw new MessageException("User must be owner of this message or admin");
        }
    }

    @Transactional
    public void updateMessage(long messageId, String text) {
        User currentUser = getCurrentUser();
        GroupMessage message = groupsMessagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Group group = message.getGroup();
        groupsService.getGroupUser(group, currentUser);
        if (message.getMessageOwner().getId() == currentUser.getId()) {
            if (message.getContentType() != ContentType.TEXT) {
                throw new MessageException("The file cannot be changed");
            }
            message.setText(text);
            messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(),
                    new ResponseMessageUpdatingDTO(messageId, text, message.getSentTime(), message.getMessageOwner().getId()));
        } else {
            throw new MessageException("User must be owner of this message");
        }
    }
}
