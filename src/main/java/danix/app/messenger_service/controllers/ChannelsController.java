package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.ContentType;
import danix.app.messenger_service.services.ChannelsPostsService;
import danix.app.messenger_service.services.ChannelsService;
import danix.app.messenger_service.util.AbstractException;
import danix.app.messenger_service.util.ErrorHandler;
import danix.app.messenger_service.util.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static danix.app.messenger_service.util.ExceptionType.CHANNEL_EXCEPTION;
import static danix.app.messenger_service.util.ExceptionType.MESSAGE_EXCEPTION;

@RestController
@RequiredArgsConstructor
@RequestMapping("/channels")
public class ChannelsController {
    private final ChannelsService channelsService;
    private final ChannelsPostsService channelsPostsService;

    @GetMapping
    public List<ResponseChannelDTO> getUserChannels() {
        return channelsService.getAllUserChannels();
    }

    @GetMapping("/users/banned/{id}")
    public List<ResponseUserDTO> getBannedUsers(@PathVariable int id) {
        return channelsService.getBannedUsers(id);
    }

    @GetMapping("/invites")
    public List<ResponseChannelInviteDTO> getInvites() {
        return channelsService.getChannelsInvites();
    }

    @GetMapping("/find")
    public ResponseEntity<ResponseChannelDTO> findChannel(@RequestParam String name) {
        return new ResponseEntity<>(channelsService.findChannel(name), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowChannelDTO> showChannel(@PathVariable int id) {
        return new ResponseEntity<>(channelsService.showChannel(id), HttpStatus.OK);
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<List<ResponseChannelPostDTO>> getChannelPosts(@PathVariable int id, @RequestParam int page,
                                                                        @RequestParam int count) {
        return new ResponseEntity<>(channelsPostsService.getByChannel(id, page, count), HttpStatus.OK);
    }

    @GetMapping("/{id}/users")
    public List<ResponseChannelUserDTO> getUsers(@PathVariable int id, @RequestParam int page, @RequestParam int count) {
        return channelsService.getUsers(id, page, count);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> getChannelImage(@PathVariable int id) {
        ResponseFileDTO image = channelsService.getImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getFileData());
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<ResponseChannelLogDTO>> showLogs(@PathVariable int id) {
        return new ResponseEntity<>(channelsService.showChannelLogs(id), HttpStatus.OK);
    }

    @GetMapping("/{id}/options")
    public ResponseEntity<ChannelsOptionsDTO> getChannelOptions(@PathVariable int id) {
        return new ResponseEntity<>(channelsService.getChannelOptions(id), HttpStatus.OK);
    }

    @GetMapping("/post/file/{id}")
    public ResponseEntity<?> getPostFile(@PathVariable long id) {
        ResponseFileDTO file = channelsPostsService.getPostFile(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(file.getType())
                .body(file.getFileData());
    }

    @GetMapping("/post/comment/{id}/file")
    public ResponseEntity<?> getCommentImage(@PathVariable long id) {
        ResponseFileDTO file = channelsPostsService.getCommentFile(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(file.getType())
                .body(file.getFileData());
    }

    @GetMapping("/post/{id}/comments")
    public ResponseEntity<List<ResponseChannelPostCommentDTO>> showComments(@PathVariable long id, @RequestParam int page,
                                                                            @RequestParam int count) {
        return ResponseEntity.ok(channelsPostsService.getPostComments(id, page, count));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<HttpStatus> joinToChannel(@PathVariable int id) {
        channelsService.joinToChannel(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/{channelId}/user/{userId}/invite")
    public ResponseEntity<HttpStatus> invite(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.inviteToChannel(channelId, userId);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/invite/{id}")
    public ResponseEntity<HttpStatus> acceptInvite(@PathVariable int id) {
        channelsService.acceptInviteToChannel(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<IdDTO> createChannel(@RequestBody @Valid CreateChannelDTO createChannelDTO,
                                               BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        long id = channelsService.createChannel(createChannelDTO);
        return new ResponseEntity<>(new IdDTO(id), HttpStatus.CREATED);
    }

    @PostMapping("/{channelId}/user/{userId}/ban")
    public ResponseEntity<HttpStatus> banUser(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.banUser(channelId, userId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/post")
    public ResponseEntity<IdDTO> createPost(@RequestBody @Valid CreateChannelPostDTO post, BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        long id = channelsPostsService.createPost(post);
        return new ResponseEntity<>(new IdDTO(id), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/post/image")
    public ResponseEntity<IdDTO> createPostWithImage(@PathVariable int id, @RequestParam MultipartFile image) {
        long postId = channelsPostsService.createPost(image, id, ContentType.IMAGE);
        return new ResponseEntity<>(new IdDTO(postId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/post/video")
    public ResponseEntity<IdDTO> createPostWithVideo(@PathVariable int id, @RequestParam MultipartFile video) {
        long postId = channelsPostsService.createPost(video, id, ContentType.VIDEO);
        return new ResponseEntity<>(new IdDTO(postId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/post/audio/ogg")
    public ResponseEntity<IdDTO> createPostWithAudioOgg(@PathVariable int id, @RequestParam MultipartFile audio) {
        long postId = channelsPostsService.createPost(audio, id, ContentType.AUDIO_OGG);
        return new ResponseEntity<>(new IdDTO(postId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/post/audio/mp3")
    public ResponseEntity<IdDTO> createPostWithAudioMP3(@PathVariable int id, @RequestParam MultipartFile audio) {
        long postId = channelsPostsService.createPost(audio, id, ContentType.AUDIO_MP3);
        return new ResponseEntity<>(new IdDTO(postId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/video")
    public ResponseEntity<IdDTO> addPostVideo(@PathVariable long id, @RequestParam("video") MultipartFile video) {
        long videoId = channelsPostsService.addFile(id, video, ContentType.VIDEO);
        return new ResponseEntity<>(new IdDTO(videoId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/image")
    public ResponseEntity<IdDTO> addPostImage(@PathVariable long id, @RequestParam("image") MultipartFile image) {
        long imageId = channelsPostsService.addFile(id, image, ContentType.IMAGE);
        return new ResponseEntity<>(new IdDTO(imageId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/audio/ogg")
    public ResponseEntity<IdDTO> addPostAudioOgg(@PathVariable long id, @RequestParam("audio") MultipartFile audio) {
        long audioId = channelsPostsService.addFile(id, audio, ContentType.AUDIO_OGG);
        return new ResponseEntity<>(new IdDTO(audioId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/audio/mp3")
    public ResponseEntity<IdDTO> addPostAudioMP3(@PathVariable long id, @RequestParam("audio") MultipartFile audio) {
        long audioId = channelsPostsService.addFile(id, audio, ContentType.AUDIO_MP3);
        return new ResponseEntity<>(new IdDTO(audioId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/like")
    public ResponseEntity<HttpStatus> likePost(@PathVariable long id) {
        channelsPostsService.addPostLike(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/post/comment")
    public ResponseEntity<IdDTO> createComment(@RequestBody @Valid CreateChannelPostCommentDTO comment,
                                               BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, MESSAGE_EXCEPTION);
        long id = channelsPostsService.createComment(comment);
        return new ResponseEntity<>(new IdDTO(id), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/comment/image")
    public ResponseEntity<IdDTO> createImageComment(@PathVariable long id, @RequestParam("image") MultipartFile image) {
        long commentId = channelsPostsService.createComment(id, image, ContentType.IMAGE);
        return new ResponseEntity<>(new IdDTO(commentId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/comment/video")
    public ResponseEntity<IdDTO> createVideoComment(@PathVariable long id, @RequestParam("video") MultipartFile video) {
        long commentId = channelsPostsService.createComment(id, video, ContentType.VIDEO);
        return new ResponseEntity<>(new IdDTO(commentId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/comment/audio/ogg")
    public ResponseEntity<IdDTO> createAudioOggComment(@PathVariable long id, @RequestParam("audio") MultipartFile audio) {
        long commentId = channelsPostsService.createComment(id, audio, ContentType.AUDIO_OGG);
        return new ResponseEntity<>(new IdDTO(commentId), HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/comment/audio/mp3")
    public ResponseEntity<IdDTO> createAudioMP3Comment(@PathVariable long id, @RequestParam("audio") MultipartFile audio) {
        long commentId = channelsPostsService.createComment(id, audio, ContentType.AUDIO_MP3);
        return new ResponseEntity<>(new IdDTO(commentId), HttpStatus.CREATED);
    }

    @PatchMapping("/post/comment/{id}")
    public ResponseEntity<HttpStatus> updateComment(@PathVariable long id, @RequestBody @Valid UpdateChannelPostCommentDTO comment,
                                                    BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, MESSAGE_EXCEPTION);
        channelsPostsService.updateComment(comment, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{id}/image")
    public ResponseEntity<HttpStatus> updateChannelImage(@PathVariable int id, @RequestParam("image") MultipartFile image) {
        channelsService.addImage(image, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/post")
    public ResponseEntity<HttpStatus> updatePost(@RequestBody @Valid UpdateChannelPostDTO post,
                                                 BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        channelsPostsService.updatePost(post);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{channelId}/user/{userId}/admin/add")
    public ResponseEntity<HttpStatus> addAdmin(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.addAdmin(channelId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{channelId}/user/{userId}/admin/delete")
    public ResponseEntity<HttpStatus> deleteAdmin(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.deleteAdmin(channelId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HttpStatus> updateChannel(@RequestBody @Valid UpdateChannelDTO updateChannelDTO,
                                                    BindingResult bindingResult, @PathVariable int id) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        channelsService.updateChannel(updateChannelDTO, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{id}/options")
    public ResponseEntity<HttpStatus> updateChannelOptions(@PathVariable int id, @RequestBody ChannelsOptionsDTO channelsOptionsDTO) {
        channelsService.updateChannelOptions(id, channelsOptionsDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/post/comment/{id}")
    public ResponseEntity<HttpStatus> deleteComment(@PathVariable long id) {
        channelsPostsService.deleteComment(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/post/{id}/like")
    public ResponseEntity<HttpStatus> deletePostLike(@PathVariable long id) {
        channelsPostsService.deletePostLike(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/post/{id}")
    public ResponseEntity<HttpStatus> deletePost(@PathVariable long id) {
        channelsPostsService.deletePost(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteChannel(@PathVariable int id) {
        channelsService.deleteChannel(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{channelId}/user/{userId}/unban")
    public ResponseEntity<HttpStatus> unbanUser(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.unbanUser(channelId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<HttpStatus> leaveFromChannel(@PathVariable int id) {
        channelsService.leaveChannel(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<HttpStatus> deleteChannelImage(@PathVariable int id) {
        channelsService.deleteImage(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}