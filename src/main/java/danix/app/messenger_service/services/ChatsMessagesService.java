package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BlockedUsersRepository;
import danix.app.messenger_service.repositories.ChatsMessagesRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.util.ChatException;
import danix.app.messenger_service.util.ImageException;
import danix.app.messenger_service.util.ImageService;
import danix.app.messenger_service.util.MessageException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
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

    @Transactional
    public void sendTextMessage(String message, int chatId) {
        sendMessage(message, ContentType.TEXT, chatId);
    }

    @Transactional
    public void sendImage(MultipartFile image, int chatId) {
        String uuid = UUID.randomUUID().toString();
        sendMessage(uuid, ContentType.IMAGE, chatId);
        ImageService.upload(Path.of(CHATS_IMAGES_PATH), image, uuid);
    }

    private void sendMessage(String message, ContentType contentType, int chatId) {
        User currentUser = getCurrentUser();
        Chat chat = chatsUsersRepository.findById(chatId)
                .orElseThrow(() -> new ChatException("Chat not found"));
        User user = chat.getUser1().getId() == currentUser.getId() ? chat.getUser2() : chat.getUser1();
        if (user.getUsername().equals(currentUser.getUsername())) {
            throw new MessageException("The user cannot send a message to himself");
        }
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresentOrElse(person -> {
            throw new MessageException("This user blocked current user");
        }, () -> blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(person -> {
            throw new MessageException("Current user has blocked this user");
        }));
        ChatMessage chatMessage = new ChatMessage();
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
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, responseChatMessageDTO);
    }

    @Transactional
    public void deleteMessage(long messageId) {
        ChatMessage message = checkMessage(messageId);
        if (message.getContentType() == ContentType.IMAGE) {
            Path path = Path.of(CHATS_IMAGES_PATH);
            ImageService.delete(path, message.getText());
        }
        messagesRepository.delete(message);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(),
                new ResponseMessageDeletionDTO(messageId));
    }

    @Transactional
    public void updateMessage(long messageId, String text) {
        ChatMessage message = checkMessage(messageId);
        if (message.getContentType() == ContentType.IMAGE) {
            throw new MessageException("The image cannot be changed");
        }
        message.setText(text);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(),
                new ResponseMessageUpdatingDTO(messageId, text));
    }

    public ResponseImageDTO getMessageImage(long messageId) {
        ChatMessage chatMessage = messagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Chat chat = chatMessage.getChat();
        User currentUser = getCurrentUser();
        if (!chat.getUser1().getUsername().equals(currentUser.getUsername()) &&
                !chat.getUser2().getUsername().equals(currentUser.getUsername())) {
            throw new ChatException("Current user not exist in this chat");
        }
        if (chatMessage.getContentType() != ContentType.IMAGE) {
            throw new ImageException("Image not found");
        }
        return ImageService.download(Path.of(CHATS_IMAGES_PATH), chatMessage.getText());
    }

    private ChatMessage checkMessage(long messageId) {
        ChatMessage message = messagesRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));
        Chat chat = message.getChat();
        if (chat.getUser2().getUsername().equals(getCurrentUser().getUsername()) ||
                chat.getUser1().getUsername().equals(getCurrentUser().getUsername())) {
            if (message.getChat().getId() == chat.getId()) {
                if (message.getOwner().getId() != getCurrentUser().getId()) {
                    throw new MessageException("User not own this message.");
                }
            } else {
                throw new MessageException("Message not exist in this chat.");
            }
        } else {
            throw new MessageException("User not exist in this chat.");
        }
        return message;
    }
}
