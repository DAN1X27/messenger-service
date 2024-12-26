package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.services.ChatsService;
import danix.app.messenger_service.services.ChatsMessagesService;
import danix.app.messenger_service.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatsController {
    private final ChatsService chatsService;
    private final ChatsMessagesService chatsMessagesService;

    @GetMapping
    public ResponseEntity<List<ResponseChatDTO>> getAllUserChats() {
        return new ResponseEntity<>(chatsService.getAllUserChats(), HttpStatus.OK);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<HttpStatus> createChat(@PathVariable int userId) {
        chatsService.createChat(userId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowChatDTO> showChat(@PathVariable int id, @RequestParam("page") int page,
                                                @RequestParam("count") int count) {
        return new ResponseEntity<>(chatsService.showChat(id, page, count), HttpStatus.OK);
    }

    @GetMapping("/message/{id}/image")
    public ResponseEntity<?> getMessageImage(@PathVariable long id) {
        ResponseImageDTO image = chatsMessagesService.getMessageImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getImageData());
    }

    @PostMapping("/{id}/message/image")
    public ResponseEntity<HttpStatus> sendImage(@RequestParam("image") MultipartFile image, @PathVariable int id) {
        chatsMessagesService.sendImage(image, id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/{id}/message")
    public ResponseEntity<HttpStatus> sendMessage(@RequestBody Map<String, String> message , @PathVariable int id) {
        if (!message.containsKey("message")) {
            throw new MessageException("Message must not be empty");
        }
        chatsMessagesService.sendTextMessage(message.get("message"), id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/message/{id}")
    public ResponseEntity<HttpStatus> deleteMessage(@PathVariable long id) {
        chatsMessagesService.deleteMessage(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/message/{id}")
    public ResponseEntity<HttpStatus> updateMessage(@RequestBody Map<String, String> message, @PathVariable long id) {
        if (!message.containsKey("message")) {
            throw new MessageException("Message must not be empty");
        }
        chatsMessagesService.updateMessage(id, message.get("message"));
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
