package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.*;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static danix.app.messenger_service.util.ExceptionType.CHANNEL_EXCEPTION;
import static danix.app.messenger_service.util.ExceptionType.MESSAGE_EXCEPTION;

@RestController
@RequiredArgsConstructor
@RequestMapping("/channels")
public class ChannelsController {
    private final ChannelsService channelsService;
    private final ChannelsPostsService channelsPostsServiceImpl;

    @GetMapping
    public ResponseEntity<List<ResponseChannelDTO>> getUserChannels() {
        return new ResponseEntity<>(channelsService.getAllUserChannels(), HttpStatus.OK);
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

    @DeleteMapping("/leave/{id}")
    public ResponseEntity<HttpStatus> leaveFromChannel(@PathVariable int id) {
        channelsService.leaveChannel(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/find")
    public ResponseEntity<ResponseChannelDTO> findChannel(@RequestBody Map<String, String> channelData) {
        if (!channelData.containsKey("name")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(channelsService.findChannel(channelData.get("name")), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowChannelDTO> showChannel(@PathVariable int id, @RequestParam("page") int page,
                                                      @RequestParam("posts") int posts) {
        return new ResponseEntity<>(channelsService.showChannel(id, page, posts), HttpStatus.OK);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> getChannelImage(@PathVariable int id) {
        ResponseImageDTO image = channelsService.getImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getImageData());
    }

    @PatchMapping("/{id}/image")
    public ResponseEntity<HttpStatus> updateChannelImage(@PathVariable int id, @RequestParam("image") MultipartFile image) {
        channelsService.addImage(image, id);
        return ResponseEntity.ok(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<HttpStatus> deleteChannelImage(@PathVariable int id) {
        channelsService.deleteImage(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<ResponseChannelLogDTO>> showLogs(@PathVariable int id) {
        return new ResponseEntity<>(channelsService.showChannelLogs(id), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<HttpStatus> createChannel(@RequestBody @Valid CreateChannelDTO createChannelDTO,
                                                    BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        channelsService.createChannel(createChannelDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteChannel(@PathVariable int id) {
        channelsService.deleteChannel(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HttpStatus> updateChannel(@RequestBody @Valid UpdateChannelDTO updateChannelDTO,
                                                    BindingResult bindingResult, @PathVariable int id) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        channelsService.updateChannel(updateChannelDTO, id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/{channelId}/user/{userId}/ban")
    public ResponseEntity<HttpStatus> banUser(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.banUser(channelId, userId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{channelId}/user/{userId}/unban")
    public ResponseEntity<HttpStatus> unbanUser(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.unbanUser(channelId, userId);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/{channelId}/user/{userId}/admin/add")
    public ResponseEntity<HttpStatus> addAdmin(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.addAdmin(channelId, userId);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/{channelId}/user/{userId}/admin/delete")
    public ResponseEntity<HttpStatus> deleteAdmin(@PathVariable int channelId, @PathVariable int userId) {
        channelsService.deleteAdmin(channelId, userId);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/post")
    public ResponseEntity<HttpStatus> createPost(@RequestBody @Valid CreateChannelPostDTO post, BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        channelsPostsServiceImpl.createPost(post);
        return ResponseEntity.ok(HttpStatus.CREATED);
    }

    @PostMapping("/{channelId}/post/image")
    public ResponseEntity<HttpStatus> createPost(@PathVariable int channelId, @RequestParam MultipartFile image) {
        channelsPostsServiceImpl.createPost(image, channelId);
        return ResponseEntity.ok(HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/image")
    public ResponseEntity<HttpStatus> addPostImage(@PathVariable long id, @RequestParam("image") MultipartFile image) {
        channelsPostsServiceImpl.addImage(id, image);
        return ResponseEntity.ok(HttpStatus.CREATED);
    }

    @GetMapping("/post/image/{imageId}")
    public ResponseEntity<?> getPostImage(@PathVariable long imageId) {
        ResponseImageDTO image = channelsPostsServiceImpl.getPostImage(imageId);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getImageData());
    }

    @PatchMapping("/post")
    public ResponseEntity<HttpStatus> updatePost(@RequestBody @Valid UpdateChannelPostDTO post,
                                                 BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, CHANNEL_EXCEPTION);
        channelsPostsServiceImpl.updatePost(post);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/post/{id}")
    public ResponseEntity<HttpStatus> deletePost(@PathVariable long id) {
        channelsPostsServiceImpl.deletePost(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/post/like/{id}")
    public ResponseEntity<HttpStatus> likePost(@PathVariable long id) {
        channelsPostsServiceImpl.addPostLike(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/post/like/{id}")
    public ResponseEntity<HttpStatus> deletePostLike(@PathVariable long id) {
        channelsPostsServiceImpl.deletePostLike(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/comment")
    public ResponseEntity<HttpStatus> createComment(@RequestBody @Valid CreateChannelPostCommentDTO comment,
                                                    BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, MESSAGE_EXCEPTION);
        channelsPostsServiceImpl.createComment(comment);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/post/{id}/comment/image")
    public ResponseEntity<HttpStatus> createComment(@PathVariable long id, @RequestParam("image") MultipartFile image) {
        channelsPostsServiceImpl.createComment(id, image);
        return ResponseEntity.ok(HttpStatus.CREATED);
    }

    @GetMapping("/post/comment/{id}/image")
    public ResponseEntity<?> getCommentImage(@PathVariable long id) {
        ResponseImageDTO image = channelsPostsServiceImpl.getCommentImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getImageData());
    }

    @DeleteMapping("/post/comments/{id}")
    public ResponseEntity<HttpStatus> deleteComment(@PathVariable long id) {
        channelsPostsServiceImpl.deleteComment(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/post/comment/{id}")
    public ResponseEntity<HttpStatus> updateComment(@PathVariable long id, @RequestBody @Valid UpdateChannelPostCommentDTO comment,
                                                    BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, MESSAGE_EXCEPTION);
        channelsPostsServiceImpl.updateComment(comment, id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/post/comments/{id}")
    public ResponseEntity<List<ResponseChannelPostCommentDTO>> showComments(@PathVariable long id) {
        return ResponseEntity.ok(channelsPostsServiceImpl.getPostComments(id));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
