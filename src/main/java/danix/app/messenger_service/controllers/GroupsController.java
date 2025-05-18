package danix.app.messenger_service.controllers;

import danix.app.messenger_service.api.GroupsAPI;
import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.ContentType;
import danix.app.messenger_service.services.GroupsMessagesService;
import danix.app.messenger_service.services.GroupsService;
import danix.app.messenger_service.util.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Groups API")
public class GroupsController implements GroupsAPI {
    private final GroupsService groupsService;
    private final GroupsMessagesService groupsMessagesService;

    @Override
    @GetMapping
    public ResponseEntity<List<ResponseGroupDTO>> getAll() {
        return new ResponseEntity<>(groupsService.getAllUserGroups(), HttpStatus.OK);
    }

    @Override
    @GetMapping("/invites")
    public ResponseEntity<List<ResponseGroupInviteDTO>> getInvites() {
        return new ResponseEntity<>(groupsService.getAllUserGroupsInvites(), HttpStatus.OK);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ShowGroupDTO> show(@PathVariable("id") int groupId) {
        return new ResponseEntity<>(groupsService.showGroup(groupId), HttpStatus.OK);
    }

    @Override
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ResponseGroupMessageDTO>> getMessages(@PathVariable int id, @RequestParam int page,
                                                                     @RequestParam int count) {
        return new ResponseEntity<>(groupsMessagesService.getMessages(id, page, count), HttpStatus.OK);
    }

    @Override
    @GetMapping("/{id}/messages/action")
    public ResponseEntity<List<ResponseGroupActionMessageDTO>> getActionMessages(@PathVariable int id,
                                                                                      @RequestParam int page,
                                                                                      @RequestParam int count) {
        return new ResponseEntity<>(groupsMessagesService.getActionMessages(id, page, count), HttpStatus.OK);
    }

    @Override
    @GetMapping("/{id}/users")
    public ResponseEntity<List<ResponseGroupUserDTO>> getUsers(@PathVariable int id, @RequestParam int page,
                                                               @RequestParam int count) {
        return new ResponseEntity<>(groupsService.getGroupUsers(id, page, count), HttpStatus.OK);
    }

    @Override
    @PostMapping
    public ResponseEntity<IdDTO> createGroup(@RequestBody @Valid CreateGroupDTO createGroupDTO, BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.GROUP_EXCEPTION);
        long id = groupsService.createGroup(createGroupDTO);
        return new ResponseEntity<>(new IdDTO(id), HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{groupId}/user/{userId}/invite")
    public ResponseEntity<IdDTO> invite(@PathVariable int groupId, @PathVariable int userId) {
        long id = groupsService.inviteUser(groupId, userId);
        return new ResponseEntity<>(new IdDTO(id), HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/invite/{id}")
    public ResponseEntity<HttpStatus> acceptInvite(@PathVariable("id") int id) {
        groupsService.acceptInviteToGroup(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{groupId}/user/{userId}/ban")
    public ResponseEntity<HttpStatus> banUser(@PathVariable int groupId, @PathVariable int userId) {
        groupsService.banUser(groupId, userId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    @DeleteMapping("/{groupId}/user/{userId}/unban")
    public ResponseEntity<HttpStatus> unbanUser(@PathVariable int groupId, @PathVariable int userId) {
        groupsService.unbanUser(groupId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{groupId}/user/{userId}/kick")
    public ResponseEntity<HttpStatus> kickUser(@PathVariable int groupId, @PathVariable int userId) {
        groupsService.kickUser(groupId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<HttpStatus> leaveFromGroup(@PathVariable("id") int group_id) {
        groupsService.leaveGroup(group_id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @GetMapping("/{id}/image")
    public ResponseEntity<?> getImage(@PathVariable int id) {
        ResponseFileDTO image = groupsService.getImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getFileData());
    }

    @Override
    @DeleteMapping("/{id}/image")
    public ResponseEntity<HttpStatus> deleteImage(@PathVariable int id) {
        groupsService.deleteImage(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Override
    @PatchMapping("/{id}/image")
    public ResponseEntity<HttpStatus> updateImage(@PathVariable int id, @RequestParam("image") MultipartFile file) {
        groupsService.addImage(file, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteGroup(@PathVariable("id") int groupId) {
        groupsService.deleteGroup(groupId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PatchMapping
    public ResponseEntity<HttpStatus> updateGroup(@RequestBody @Valid UpdateGroupDTO updateGroupDTO,
                                                  BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.GROUP_EXCEPTION);
        groupsService.updateGroup(updateGroupDTO);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Override
    @PatchMapping("/{groupId}/user/{userId}/admin/add")
    public ResponseEntity<HttpStatus> addAdmin(@PathVariable int groupId, @PathVariable int userId) {
        groupsService.addAdmin(groupId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PatchMapping("/{groupId}/user/{userId}/admin/delete")
    public ResponseEntity<HttpStatus> deleteAdmin(@PathVariable int groupId, @PathVariable int userId) {
        groupsService.deleteAdmin(groupId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PostMapping("/{id}/message")
    public ResponseEntity<IdDTO> sendMessage(@PathVariable int id, @RequestBody @Valid MessageDTO messageDTO,
                                             BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.GROUP_EXCEPTION);
        long messageId = groupsMessagesService.sendTextMessage(messageDTO.getMessage(), id);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{id}/message/image")
    public ResponseEntity<IdDTO> sendImage(@PathVariable int id, @RequestParam("image") MultipartFile file) {
        long messageId = groupsMessagesService.sendFile(file, id, ContentType.IMAGE);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{id}/message/video")
    public ResponseEntity<IdDTO> sendVideo(@PathVariable int id, @RequestParam("video") MultipartFile file) {
        long messageId = groupsMessagesService.sendFile(file, id, ContentType.VIDEO);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{id}/message/audio/ogg")
    public ResponseEntity<IdDTO> sendAudioOgg(@PathVariable int id, @RequestParam("audio") MultipartFile file) {
        long messageId = groupsMessagesService.sendFile(file, id, ContentType.AUDIO_OGG);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{id}/message/audio/mp3")
    public ResponseEntity<IdDTO> sendAudioMP3(@PathVariable int id, @RequestParam("audio") MultipartFile file) {
        long messageId = groupsMessagesService.sendFile(file, id, ContentType.AUDIO_MP3);
        return new ResponseEntity<>(new IdDTO(messageId), HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/message/{id}/file")
    public ResponseEntity<?> getMessageFile(@PathVariable long id) {
        ResponseFileDTO image = groupsMessagesService.getFile(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getFileData());
    }

    @Override
    @PatchMapping("/message/{id}")
    public ResponseEntity<HttpStatus> updateMessage(@PathVariable long id, @RequestBody @Valid MessageDTO messageDTO,
                                                    BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.GROUP_EXCEPTION);
        groupsMessagesService.updateMessage(id, messageDTO.getMessage());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/message/{id}")
    public ResponseEntity<HttpStatus> deleteMessage(@PathVariable long id) {
        groupsMessagesService.deleteMessage(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}