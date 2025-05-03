package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.GroupsActionsMessagesRepository;
import danix.app.messenger_service.repositories.GroupsMessagesRepository;
import danix.app.messenger_service.util.FileException;
import danix.app.messenger_service.util.FilesUtils;
import danix.app.messenger_service.util.GroupException;
import danix.app.messenger_service.util.MessageException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Service
@RequiredArgsConstructor
public class GroupsMessagesService {
    private final GroupsMessagesRepository messagesRepository;
    private final GroupsActionsMessagesRepository actionsMessagesRepository;
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
    public long sendFile(MultipartFile image, int groupId, ContentType contentType) {
        if (contentType != ContentType.IMAGE && contentType != ContentType.VIDEO &&
            contentType != ContentType.AUDIO_MP3 && contentType != ContentType.AUDIO_OGG) {
            throw new MessageException("Unsupported content type");
        }
        String uuid = UUID.randomUUID().toString();
        switch (contentType) {
            case IMAGE -> FilesUtils.upload(Path.of(GROUPS_MESSAGES_IMAGES_PATH), image, uuid, contentType);
            case VIDEO -> FilesUtils.upload(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), image, uuid, contentType);
            case AUDIO_MP3, AUDIO_OGG -> FilesUtils.upload(Path.of(GROUPS_MESSAGES_AUDIO_PATH), image, uuid, contentType);
        }
        try {
            return sendMessage(uuid, groupId, contentType);
        } catch (GroupException e) {
            FilesUtils.delete(Path.of(GROUPS_MESSAGES_IMAGES_PATH), uuid);
            FilesUtils.delete(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), uuid);
            throw e;
        }
    }

    @Transactional
    public long sendTextMessage(String message, int groupId) {
        return sendMessage(message, groupId, ContentType.TEXT);
    }

    public ResponseFileDTO getFile(long messageId) {
        GroupMessage groupMessage = messagesRepository.findById(messageId)
                .orElseThrow(() -> new FileException("Image not found"));
        Group group = groupMessage.getGroup();
        groupsService.getGroupUser(group, getCurrentUser());
        switch (groupMessage.getContentType()) {
            case IMAGE -> {
                return FilesUtils.download(Path.of(GROUPS_MESSAGES_IMAGES_PATH), groupMessage.getText(), groupMessage.getContentType());
            }
            case VIDEO -> {
                return FilesUtils.download(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), groupMessage.getText(), groupMessage.getContentType());
            }
            case AUDIO_OGG, AUDIO_MP3 -> {
                return FilesUtils.download(Path.of(GROUPS_MESSAGES_AUDIO_PATH), groupMessage.getText(), groupMessage.getContentType());
            }
            default -> throw new MessageException("Message is not file");
        }
    }

    private long sendMessage(String message, int groupId, ContentType contentType) {
        User currentUser = getCurrentUser();
        Group group = groupsService.getById(groupId);
        groupsService.getGroupUser(group, currentUser);
        GroupMessage groupMessage = GroupMessage.builder()
                .text(message)
                .group(group)
                .contentType(contentType)
                .messageOwner(currentUser)
                .sentTime(LocalDateTime.now())
                .build();
        messagesRepository.save(groupMessage);
        ResponseGroupMessageDTO messageDTO = modelMapper.map(groupMessage, ResponseGroupMessageDTO.class);
        messageDTO.setText(message);
        messageDTO.setSender(modelMapper.map(currentUser, ResponseUserDTO.class));
        messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(), messageDTO);
        return groupMessage.getId();
    }

    @Transactional
    public void deleteMessage(long messageId) {
        User currentUser = getCurrentUser();
        GroupMessage message = getById(messageId);
        Group group = message.getGroup();
        GroupUser groupUser = groupsService.getGroupUser(group, currentUser);
        if (message.getMessageOwner().getId() == currentUser.getId() || groupUser.isAdmin()) {
            switch (message.getContentType()) {
                case IMAGE -> FilesUtils.delete(Path.of(GROUPS_MESSAGES_IMAGES_PATH), message.getText());
                case VIDEO -> FilesUtils.delete(Path.of(GROUPS_MESSAGES_VIDEOS_PATH), message.getText());
                case AUDIO_MP3, AUDIO_OGG -> FilesUtils.delete(Path.of(GROUPS_MESSAGES_AUDIO_PATH), message.getText());
            }
            messagesRepository.delete(message);
            messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(),
                    Map.of("deleted_message_id", messageId));
        } else {
            throw new MessageException("User must be owner of this message or admin");
        }
    }

    @Transactional
    public void updateMessage(long messageId, String text) {
        User currentUser = getCurrentUser();
        GroupMessage message = getById(messageId);
        Group group = message.getGroup();
        groupsService.getGroupUser(group, currentUser);
        if (message.getMessageOwner().getId() == currentUser.getId()) {
            if (message.getContentType() != ContentType.TEXT) {
                throw new MessageException("The file cannot be changed");
            }
            message.setText(text);
            ResponseMessageUpdatingDTO response = modelMapper.map(message, ResponseMessageUpdatingDTO.class);
            response.setSenderId(currentUser.getId());
            messagingTemplate.convertAndSend("/topic/group/" + group.getWebSocketUUID(), response);
        } else {
            throw new MessageException("User must be owner of this message");
        }
    }

    public List<ResponseGroupMessageDTO> getMessages(int id, int page, int count) {
        Group group = groupsService.getById(id);
        return messagesRepository.findAllByGroup(group, PageRequest.of(page, count,
                Sort.by(Sort.Direction.DESC, "id"))).stream()
                .map(message -> {
                    ResponseGroupMessageDTO messageDTO = modelMapper.map(message, ResponseGroupMessageDTO.class);
                    if (message.getContentType() != ContentType.TEXT) {
                        messageDTO.setText(null);
                    }
                    messageDTO.setSender(modelMapper.map(message.getMessageOwner(), ResponseUserDTO.class));
                    return messageDTO;
                })
                .toList();
    }

    public List<ResponseGroupActionMessageDTO> getActionMessages(int id, int page, int count) {
        Group group = groupsService.getById(id);
        return actionsMessagesRepository.findAllByGroup(group, PageRequest.of(page, count,
                Sort.by(Sort.Direction.DESC, "id"))).stream()
                .map(message -> modelMapper.map(message, ResponseGroupActionMessageDTO.class))
                .toList();
    }

    private GroupMessage getById(long id) {
        return messagesRepository.findById(id).orElseThrow(() -> new MessageException("Message not found"));
    }
}
