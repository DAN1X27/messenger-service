package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BlockedUsersRepository;
import danix.app.messenger_service.repositories.ChatsMessagesRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.util.ChatException;
import danix.app.messenger_service.util.FileUtils;
import danix.app.messenger_service.util.MessageException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
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
    public void sendTextMessage(String message, int chatId) {
        sendMessage(message, ContentType.TEXT, chatId);
    }

    @Transactional
    public void sendFile(MultipartFile file, int chatId, ContentType contentType) {
        String uuid = UUID.randomUUID().toString();
        switch (contentType) {
            case IMAGE -> FileUtils.upload(Path.of(CHATS_IMAGES_PATH), file, uuid, contentType);
            case VIDEO -> FileUtils.upload(Path.of(CHATS_VIDEOS_PATH), file, uuid, contentType);
            case AUDIO_OGG, AUDIO_MP3 -> FileUtils.upload(Path.of(CHATS_AUDIO_PATH), file, uuid, contentType);
            default -> throw new MessageException("Unsupported content type");
        }
        try {
            sendMessage(uuid, contentType, chatId);
        } catch (MessageException e) {
            FileUtils.delete(Path.of(CHATS_IMAGES_PATH), uuid);
            FileUtils.delete(Path.of(CHATS_VIDEOS_PATH), uuid);
            throw e;
        }
    }

    private void sendMessage(String message, ContentType contentType, int chatId) {
        User currentUser = getCurrentUser();
        Chat chat = chatsUsersRepository.findById(chatId)
                .orElseThrow(() -> new ChatException("Chat not found"));
        User user = chat.getUser1().getId() == currentUser.getId() ? chat.getUser2() : chat.getUser1();
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresentOrElse(person -> {
            throw new MessageException("Current user blocked by this user");
        }, () -> blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(person -> {
            throw new MessageException("Current user has blocked this user");
        }));
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChat(chat);
        chatMessage.setText(message);
        chatMessage.setContentType(contentType);
        chatMessage.setOwner(currentUser);
        messagesRepository.save(chatMessage);

        ResponseChatMessageDTO responseChatMessageDTO = ResponseChatMessageDTO.builder()
                .message(message)
                .messageId(chatMessage.getId())
                .sender(modelMapper.map(chatMessage.getOwner(), ResponseUserDTO.class))
                .isRead(true)
                .type(contentType)
                .sentTime(chatMessage.getSentTime())
                .build();
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getWebSocketUUID(), responseChatMessageDTO);
    }

    @Transactional
    public void deleteMessage(long messageId) {
        ChatMessage message = checkMessage(messageId);
        switch (message.getContentType()) {
            case IMAGE -> FileUtils.delete(Path.of(CHATS_IMAGES_PATH), message.getText());
            case VIDEO -> FileUtils.delete(Path.of(CHATS_VIDEOS_PATH), message.getText());
            case AUDIO_MP3, AUDIO_OGG -> FileUtils.delete(Path.of(CHATS_AUDIO_PATH), message.getText());
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
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getWebSocketUUID(),
                new ResponseMessageUpdatingDTO(messageId, text));
    }

    public ResponseFileDTO getMessageFile(long messageId) {
        ChatMessage chatMessage = messagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Chat chat = chatMessage.getChat();
        User currentUser = getCurrentUser();
        if (chat.getUser1().getId() != currentUser.getId() && chat.getUser2().getId() != currentUser.getId()) {
            throw new ChatException("Current user not exist in this chat");
        }
        switch (chatMessage.getContentType()) {
            case IMAGE -> {
                return FileUtils.download(Path.of(CHATS_IMAGES_PATH), chatMessage.getText(), ContentType.IMAGE);
            }
            case VIDEO -> {
                return FileUtils.download(Path.of(CHATS_VIDEOS_PATH), chatMessage.getText(), ContentType.VIDEO);
            }
            case AUDIO_MP3, AUDIO_OGG -> {
                return FileUtils.download(Path.of(CHATS_AUDIO_PATH), chatMessage.getText(), chatMessage.getContentType());
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
                throw new MessageException("Current user not own this message.");
            }
        } else {
            throw new MessageException("Curren user not exist in this chat.");
        }
        return message;
    }
}
