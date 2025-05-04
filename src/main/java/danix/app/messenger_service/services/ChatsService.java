package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.ChatMessage;
import danix.app.messenger_service.models.ContentType;
import danix.app.messenger_service.models.User;
import danix.app.messenger_service.models.Chat;
import danix.app.messenger_service.repositories.BlockedUsersRepository;
import danix.app.messenger_service.repositories.ChatsMessagesRepository;
import danix.app.messenger_service.repositories.ChatsRepository;
import danix.app.messenger_service.util.ChatException;
import danix.app.messenger_service.util.FilesUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatsService {
    private final UserService userService;
    private final ChatsRepository chatsRepository;
    private final ChatsMessagesRepository messagesRepository;
    private final BlockedUsersRepository blockedUsersRepository;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    @Value("${chats_images_path}")
    private String IMAGES_PATH;
    @Value("${chats_videos_path}")
    private String VIDEOS_PATH;
    @Value("${chats_audio_path}")
    private String AUDIO_PATH;

    @Transactional
    public ShowChatDTO showChat(int id, int page, int count) {
        User currentUser = getCurrentUser();
        Chat chat = chatsRepository.findById(id)
                .orElseThrow(() -> new ChatException("Chat not found"));
        if (chat.getUser1().getId() != currentUser.getId() && chat.getUser2().getId() != currentUser.getId()) {
            throw new ChatException("User not exist in this chat");
        }
        List<ChatMessage> messages = messagesRepository.findAllByChat(chat,
                PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "id")));
        messages.forEach(message -> message.setRead(true));
        return ShowChatDTO.builder()
                .id(chat.getId())
                .messages(messages.stream()
                        .map(message -> {
                            ResponseChatMessageDTO messageDTO = modelMapper.map(message, ResponseChatMessageDTO.class);
                            if (message.getContentType() != ContentType.TEXT) {
                                messageDTO.setText(null);
                            }
                            messageDTO.setSender(modelMapper.map(message.getOwner(), ResponseUserDTO.class));
                            return messageDTO;
                        })
                        .toList())
                .webSocketUUID(chat.getWebSocketUUID())
                .user(modelMapper.map(chat.getUser1().getId() == getCurrentUser().getId() ? chat.getUser2() : chat.getUser1(),
                        ResponseUserDTO.class))
                .build();
    }

    @Transactional
    public long createChat(int userId) {
        User currentUser = getCurrentUser();
        User user = userService.getById(userId);
        if (user.getId() == currentUser.getId()) {
            throw new ChatException("Invalid operation");
        }
        chatsRepository.findByUser1AndUser2(currentUser, user).ifPresentOrElse(chat -> {
            throw new ChatException("Chat already exist");
        }, () -> chatsRepository.findByUser1AndUser2(user, currentUser).ifPresent(chat -> {
            throw new ChatException("Chat already exist");
        }));
        blockedUsersRepository.findByOwnerAndBlockedUser(user, currentUser).ifPresentOrElse(blockedUser -> {
            throw new ChatException("The user blocked you");
        }, () -> blockedUsersRepository.findByOwnerAndBlockedUser(currentUser, user).ifPresent(blockedUser -> {
            throw new ChatException("You have blocked this user");
        }));
        if (user.getIsPrivate()) {
            if (userService.findUserFriend(user, currentUser) == null) {
                throw new ChatException("User has a private account");
            }
        }
        Chat chat = new Chat(currentUser, user, UUID.randomUUID().toString());
        chatsRepository.save(chat);
        ResponseChatCreatedDTO responseChatDTO = new ResponseChatCreatedDTO();
        responseChatDTO.setCreatedChatId(chat.getId());
        responseChatDTO.setUser(modelMapper.map(currentUser, ResponseUserDTO.class));
        messagingTemplate.convertAndSend("/topic/user/" + user.getWebSocketUUID() + "/main", responseChatDTO);
        return chat.getId();
    }

    @Transactional
    public CompletableFuture<Void> deleteChat(int id) {
        Chat chat = getById(id);
        checkUserInChat(chat);
        return CompletableFuture.runAsync(() -> {
           List<ChatMessage> messages;
           int page = 0;
           do {
               messages = messagesRepository.findAllByChatAndContentTypeIsNot(chat, ContentType.TEXT,
                       PageRequest.of(page, 50));
               for (ChatMessage message : messages) {
                   switch (message.getContentType()) {
                       case IMAGE -> FilesUtils.delete(Path.of(IMAGES_PATH), message.getText());
                       case VIDEO -> FilesUtils.delete(Path.of(VIDEOS_PATH), message.getText());
                       case AUDIO_MP3, AUDIO_OGG -> FilesUtils.delete(Path.of(AUDIO_PATH), message.getText());
                   }
               }
               page++;
           } while (!messages.isEmpty());
           chatsRepository.deleteById(id);
           messagingTemplate.convertAndSend("/topic/chat/" + chat.getWebSocketUUID(),
                    Map.of("deleted", true));
        });
    }

    public List<ResponseChatDTO> getAllUserChats() {
        User currentUser = getCurrentUser();
        return chatsRepository.findByUser1OrUser2(currentUser, currentUser).stream()
                .map(chat -> {
                    ResponseChatDTO responseChatDTO = new ResponseChatDTO();
                    User user = chat.getUser1().getId() == getCurrentUser().getId() ? chat.getUser2() : chat.getUser1();
                    responseChatDTO.setUser(modelMapper.map(user, ResponseUserDTO.class));
                    responseChatDTO.setId(chat.getId());
                    return responseChatDTO;
                })
                .toList();
    }

    private Chat getById(int id) {
        return chatsRepository.findById(id).orElseThrow(() -> new ChatException("Chat not found"));
    }

    private void checkUserInChat(Chat chat) {
        User curretnUser = getCurrentUser();
        if (chat.getUser1().getId() != curretnUser.getId() && chat.getUser2().getId() != curretnUser.getId()) {
            throw new ChatException("You are not in this chat");
        }
    }
}
