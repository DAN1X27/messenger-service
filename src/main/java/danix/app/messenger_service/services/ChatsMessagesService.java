package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BlockedUsersRepository;
import danix.app.messenger_service.repositories.ChatsMessagesRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.util.ChatException;
import danix.app.messenger_service.util.FilesUtils;
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
@Transactional(readOnly = true)
public class ChatsMessagesService {
    private final ChatsMessagesRepository messagesRepository;
    private final ChatsRepository chatsUsersRepository;
    private final BlockedUsersRepository blockedUsersRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;

    @Value("${chats_images_path}")
    private String CHATS_IMAGES_PATH;
    @Value("${chats_videos_path}")
    private String CHATS_VIDEOS_PATH;
    @Value("${chats_audio_path}")
    private String CHATS_AUDIO_PATH;

    @Transactional
    public long sendTextMessage(String message, int chatId) {
       return sendMessage(message, ContentType.TEXT, chatId);
    }

    @Transactional
    public long sendFile(MultipartFile file, int chatId, ContentType contentType) {
        String uuid = UUID.randomUUID().toString();
        String path = switch (contentType) {
            case IMAGE -> CHATS_IMAGES_PATH;
            case VIDEO -> CHATS_VIDEOS_PATH;
            case AUDIO_OGG, AUDIO_MP3 -> CHATS_AUDIO_PATH;
            default -> throw new MessageException("Unsupported content type");
        };
        FilesUtils.upload(Path.of(path), file, uuid, contentType);
        try {
            return sendMessage(uuid, contentType, chatId);
        } catch (MessageException e) {
            FilesUtils.delete(Path.of(CHATS_IMAGES_PATH), uuid);
            FilesUtils.delete(Path.of(CHATS_VIDEOS_PATH), uuid);
            throw e;
        }
    }

    private long sendMessage(String message, ContentType contentType, int chatId) {
        User currentUser = getCurrentUser();
        Chat chat = chatsUsersRepository.findById(chatId)
                .orElseThrow(() -> new ChatException("Chat not found"));
        User user = chat.getUser1().getId() == currentUser.getId() ? chat.getUser2() : chat.getUser1();
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresentOrElse(person -> {
            throw new MessageException("This user has blocked you");
        }, () -> blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(person -> {
            throw new MessageException("You have blocked this user");
        }));
        ChatMessage chatMessage = ChatMessage.builder()
                .text(message)
                .owner(currentUser)
                .contentType(contentType)
                .sentTime(LocalDateTime.now())
                .chat(chat)
                .isRead(false)
                .build();
        messagesRepository.save(chatMessage);
        ResponseChatMessageDTO messageDTO = modelMapper.map(chatMessage, ResponseChatMessageDTO.class);
        messageDTO.setSender(modelMapper.map(chatMessage.getOwner(), ResponseUserDTO.class));
        if (contentType != ContentType.TEXT) {
            messageDTO.setText(null);
        }
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getWebSocketUUID(), messageDTO);
        return chatMessage.getId();
    }

    @Transactional
    public void deleteMessage(long messageId) {
        ChatMessage message = checkMessage(messageId);
        switch (message.getContentType()) {
            case IMAGE -> FilesUtils.delete(Path.of(CHATS_IMAGES_PATH), message.getText());
            case VIDEO -> FilesUtils.delete(Path.of(CHATS_VIDEOS_PATH), message.getText());
            case AUDIO_MP3, AUDIO_OGG -> FilesUtils.delete(Path.of(CHATS_AUDIO_PATH), message.getText());
        }
        messagesRepository.delete(message);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getWebSocketUUID(),
                Map.of("deleted_message_id", messageId));
    }

    @Transactional
    public void updateMessage(long messageId, String text) {
        ChatMessage message = checkMessage(messageId);
        if (message.getContentType() == ContentType.IMAGE) {
            throw new MessageException("The image cannot be changed");
        }
        message.setText(text);
        ResponseMessageUpdatingDTO response = modelMapper.map(message, ResponseMessageUpdatingDTO.class);
        response.setSenderId(message.getOwner().getId());
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getWebSocketUUID(), response);
    }

    public ResponseFileDTO getMessageFile(long messageId) {
        ChatMessage chatMessage = messagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Chat chat = chatMessage.getChat();
        User currentUser = getCurrentUser();
        if (chat.getUser1().getId() != currentUser.getId() && chat.getUser2().getId() != currentUser.getId()) {
            throw new ChatException("You are not in this chat");
        }
        switch (chatMessage.getContentType()) {
            case IMAGE -> {
                return FilesUtils.download(Path.of(CHATS_IMAGES_PATH), chatMessage.getText(), ContentType.IMAGE);
            }
            case VIDEO -> {
                return FilesUtils.download(Path.of(CHATS_VIDEOS_PATH), chatMessage.getText(), ContentType.VIDEO);
            }
            case AUDIO_MP3, AUDIO_OGG -> {
                return FilesUtils.download(Path.of(CHATS_AUDIO_PATH), chatMessage.getText(), chatMessage.getContentType());
            }
            default -> throw new MessageException("Message is not file");
        }
    }

    private ChatMessage checkMessage(long messageId) {
        User currentUser = getCurrentUser();
        ChatMessage message = messagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Chat chat = message.getChat();
        if (chat.getUser1().getId() == currentUser.getId() || chat.getUser2().getId() == currentUser.getId()) {
            if (message.getOwner().getId() != currentUser.getId()) {
                throw new MessageException("You are not own this message");
            }
        } else {
            throw new MessageException("You are not in this chat");
        }
        return message;
    }
}
