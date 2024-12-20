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
import danix.app.messenger_service.util.ImageException;
import danix.app.messenger_service.util.MessageException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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

    @Value("${chats_images_path}")
    private String CHATS_IMAGES_PATH;

    @Transactional
    public ShowChatDTO showChat(int id, int page, int count) {
        Chat chat = chatsRepository.findById(id)
                .orElseThrow(() -> new ChatException("Chat not found"));
        User currentUser = getCurrentUser();
        if (chat.getUser1().getId() != currentUser.getId() && chat.getUser2().getId() != currentUser.getId()) {
            throw new ChatException("User not exist in this chat");
        }
        chat.getMessages().forEach(message -> {
            if (!message.isRead() && message.getOwner().getId() != currentUser.getId()) {
                message.setRead(true);
            }
        });
        return convertToShowChatDTO(chat, page, count);
    }

    @Transactional
    public void createChat(String username) {
        User user = userService.getByUsername(username);
        chatsRepository.findByUser1AndUser2(getCurrentUser(), user)
                .ifPresentOrElse(chat -> {
                    throw new ChatException("Chat already exist");
                }, () -> {
                    chatsRepository.findByUser1AndUser2(user, getCurrentUser()).ifPresent(chat -> {
                        throw new ChatException("Chat already exist");
                    });
                });
        blockedUsersRepository.findByOwnerAndBlockedUser(user, getCurrentUser()).ifPresentOrElse(blockedUser -> {
            throw new ChatException("User blocked you");
        }, () -> {
            blockedUsersRepository.findByOwnerAndBlockedUser(getCurrentUser(), user).ifPresent(blockedUser -> {
                throw new ChatException("Current user hase blocked this user");
            });
        });
        chatsRepository.save(new Chat(getCurrentUser(), user));
    }

    public ResponseImageDTO getChatImage(long messageId) {
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
        return ImageService.download(Path.of(CHATS_IMAGES_PATH), chatMessage.getMessage());
    }

    public List<ResponseChatDTO> getAllUserChats() {
        return chatsRepository.findByUser1OrUser2(getCurrentUser(), getCurrentUser()).stream()
                .map(this::convertToResponseChatDTO)
                .collect(Collectors.toList());
    }

    private ShowChatDTO convertToShowChatDTO(Chat chat, int page, int count) {
        ShowChatDTO showChatDTO = new ShowChatDTO();
        showChatDTO.setMessages(messagesRepository.findAllByChat(chat, PageRequest.of(page, count)).stream()
                .map(this::convertToMessageDTO)
                .toList());
        showChatDTO.setUser1Username(chat.getUser1().getUsername());
        showChatDTO.setUser2Username(chat.getUser2().getUsername());
        showChatDTO.setId(chat.getId());
        return showChatDTO;
    }

    private ResponseChatDTO convertToResponseChatDTO(Chat chat) {
        ResponseChatDTO responseChatDTO = new ResponseChatDTO();
        ResponseUserDTO responseUserDTO = new ResponseUserDTO();
        if (chat.getUser1().getUsername().equals(getCurrentUser().getUsername())) {
            responseUserDTO.setUsername(chat.getUser2().getUsername());
            responseUserDTO.setId(chat.getUser2().getId());
        } else {
            responseUserDTO.setUsername(chat.getUser1().getUsername());
            responseUserDTO.setId(chat.getUser1().getId());
        }
        responseChatDTO.setUser(responseUserDTO);
        responseChatDTO.setId(chat.getId());
        return responseChatDTO;
    }

    private ResponseChatMessageDTO convertToMessageDTO(ChatMessage chatMessage) {
        return ResponseChatMessageDTO.builder()
                .message(chatMessage.getMessage())
                .messageId(chatMessage.getId())
                .sender(modelMapper.map(chatMessage.getOwner(), ResponseUserDTO.class))
                .sentTime(chatMessage.getSentTime())
                .type(chatMessage.getContentType())
                .isRead(chatMessage.isRead())
                .build();
    }
}
