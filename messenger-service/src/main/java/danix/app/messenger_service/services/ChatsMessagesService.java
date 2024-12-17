package danix.app.messenger_service.services;

import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BlockedUsersRepository;
import danix.app.messenger_service.repositories.ChatsMessagesRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.util.MessageException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final ChatsMessagesRepository chatsMessagesRepository;
    private final ChatsRepository chatsUsersRepository;
    private final UserService userService;
    private final BlockedUsersRepository blockedUsersRepository;

    @Value("${chats_images_path}")
    private String CHATS_IMAGES_PATH;

    @Transactional
    public void sendTextMessage(String message, int id) {
        sendMessage(message, ContentType.TEXT, id);
    }

    @Transactional
    public void sendImage(MultipartFile image, int userId) {
        String uuid = UUID.randomUUID().toString();
        sendMessage(uuid, ContentType.IMAGE, userId);
        ImageService.upload(Path.of(CHATS_IMAGES_PATH), image, uuid);
    }

    @Transactional
    public void sendMessage(String message, ContentType contentType, int userId) {
        User currentUser = getCurrentUser();
        User user = userService.getById(userId);
        if (user.getUsername().equals(currentUser.getUsername())) {
            throw new MessageException("The user cannot send a message to himself");
        }
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresentOrElse(person -> {
            throw new MessageException("This user blocked current user");
        }, () -> blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(person -> {
            throw new MessageException("Current user has blocked this user");
        }));
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setContentType(contentType);
        chatMessage.setOwner(currentUser);
        chatsUsersRepository.findByUser1AndUser2(currentUser, user)
                .ifPresentOrElse(chatMessage::setChat, () -> chatsUsersRepository.findByUser1AndUser2(user, currentUser)
                        .ifPresentOrElse(chatMessage::setChat, () -> {
                            Chat chat = new Chat();
                            chat.setUser1(currentUser);
                            chat.setUser2(user);
                            chatsUsersRepository.save(chat);
                            chatMessage.setChat(chat);
                        }));
        chatsMessagesRepository.save(chatMessage);
    }

    @Transactional
    public void deleteMessage(long messageId) {
        ChatMessage message = checkMessage(messageId);
        if (message.getContentType() == ContentType.IMAGE) {
            Path path = Path.of(CHATS_IMAGES_PATH);
            ImageService.delete(path, message.getMessage());
        }
        chatsMessagesRepository.delete(message);
    }

    @Transactional
    public void updateMessage(long messageId, String text) {
        ChatMessage message = checkMessage(messageId);
        if (message.getContentType() == ContentType.IMAGE) {
            throw new MessageException("The image cannot be changed");
        }
        message.setMessage(text);
    }

    private ChatMessage checkMessage(long messageId) {
        ChatMessage message = chatsMessagesRepository.findById(messageId)
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
