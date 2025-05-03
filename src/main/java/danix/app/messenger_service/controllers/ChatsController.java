package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.ContentType;
import danix.app.messenger_service.services.ChatsService;
import danix.app.messenger_service.services.ChatsMessagesService;
import danix.app.messenger_service.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<ShowChatDTO> showChat(@PathVariable int id, @RequestParam("page") int page,
                                                @RequestParam("count") int count) {
        return new ResponseEntity<>(chatsService.showChat(id, page, count), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteChat(@PathVariable int id) {
        chatsService.deleteChat(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/message/{id}/file")
    public ResponseEntity<?> getMessageFile(@PathVariable long id) {
        ResponseFileDTO image = chatsMessagesService.getMessageFile(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getFileData());
    }

    @PostMapping("/{userId}")
    public ResponseEntity<IdDTO> createChat(@PathVariable int userId) {
        long id = chatsService.createChat(userId);
        return new ResponseEntity<>(new IdDTO(id), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/message/image")
    public ResponseEntity<IdDTO> sendImage(@RequestParam("image") MultipartFile image, @PathVariable int id) {
        long messageId = chatsMessagesService.sendFile(image, id, ContentType.IMAGE);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.OK);
    }

    @PostMapping("/{id}/message/video")
    public ResponseEntity<IdDTO> sendVideo(@RequestParam("video") MultipartFile video, @PathVariable int id) {
        long messageId = chatsMessagesService.sendFile(video, id, ContentType.VIDEO);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/message/audio/mp3")
    public ResponseEntity<IdDTO> sendAudioMP3(@RequestParam("audio") MultipartFile audio, @PathVariable int id) {
        long messageId = chatsMessagesService.sendFile(audio, id, ContentType.AUDIO_MP3);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/message/audio/ogg")
    public ResponseEntity<IdDTO> sendAudioOgg(@RequestParam("audio") MultipartFile audio, @PathVariable int id) {
        long messageId = chatsMessagesService.sendFile(audio, id, ContentType.AUDIO_OGG);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/message")
    public ResponseEntity<IdDTO> sendMessage(@PathVariable int id,@RequestBody @Valid MessageDTO messageDTO,
                                             BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.CHAT_EXCEPTION);
        long messageId = chatsMessagesService.sendTextMessage(messageDTO.getMessage(), id);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @PatchMapping("/message/{id}")
    public ResponseEntity<HttpStatus> updateMessage(@PathVariable long id, @RequestBody @Valid MessageDTO messageDTO,
                                                    BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.CHAT_EXCEPTION);
        chatsMessagesService.updateMessage(id, messageDTO.getMessage());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/message/{id}")
    public ResponseEntity<HttpStatus> deleteMessage(@PathVariable long id) {
        chatsMessagesService.deleteMessage(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}