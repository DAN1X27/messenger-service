package danix.app.messenger_service.api;

import danix.app.messenger_service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChannelsAPI {

    @Operation(summary = "Returns all user's channels")
    ResponseEntity<List<ResponseChannelDTO>> getUserChannels();

    @Operation(
            summary = "Finds channels by name",
            description = "Finds all channels by name and returns them"
    )
    ResponseEntity<ResponseChannelDTO> findChannel(@Parameter(description = "Channel name", example = "channel 1") String name);

    @Operation(
            summary = "Returns channel info",
            description = "Finds channel by id and returns its info"
    )
    ResponseEntity<ShowChannelDTO> showChannel(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Creates channel")
    @ApiResponse(description = "Channel id")
    ResponseEntity<IdDTO> createChannel(CreateChannelDTO createChannelDTO, BindingResult bindingResult);

    @Operation(summary = "Joins user to channel")
    ResponseEntity<HttpStatus> joinToChannel(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(
            summary = "Returns all invites",
            description = "Returns all user's invites to join the channel"
    )
    ResponseEntity<List<ResponseChannelInviteDTO>> getInvites();

    @Operation(summary = "Invites user to channel")
    ResponseEntity<HttpStatus> inviteToChannel(@Parameter(description = "Channel id", example = "10") int channelId,
                                      @Parameter(description = "User id", example = "20") int userId);

    @Operation(
            summary = "Accepts invite to channel",
            description = "Finds invite by user and channel and accepts it"
    )
    ResponseEntity<HttpStatus> acceptInvite(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Deletes channel")
    ResponseEntity<HttpStatus> deleteChannel(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Updates channel")
    ResponseEntity<HttpStatus> updateChannel(UpdateChannelDTO updateChannelDTO, BindingResult bindingResult,
                                             @Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Returns channel image")
    ResponseEntity<?> getChannelImage(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Updates channel's image")
    ResponseEntity<HttpStatus> updateChannelImage(@Parameter(description = "Channel id", example = "10") int id,
                                                  @RequestBody(content = {
                                                          @Content(mediaType = "image/jpeg"),
                                                          @Content(mediaType = "image/png")
                                                  }) MultipartFile image);

    @Operation(summary = "Deletes channel's image")
    ResponseEntity<HttpStatus> deleteChannelImage(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Bans user in channel")
    ResponseEntity<HttpStatus> banUser(@Parameter(description = "Channel id", example = "10") int channelId,
                                       @Parameter(description = "User id", example = "20") int userId);

    @Operation(summary = "Unbans user in channel")
    ResponseEntity<HttpStatus> unbanUser(@Parameter(description = "Channel id", example = "10") int channelId,
                                         @Parameter(description = "User id", example = "20") int userId);

    @Operation(description = "Adds admin to channel")
    ResponseEntity<HttpStatus> addAdmin(@Parameter(description = "Channel id", example = "10") int channelId,
                                        @Parameter(description = "User id", example = "20") int userId);

    @Operation(description = "Deletes admin in channel")
    ResponseEntity<HttpStatus> deleteAdmin(@Parameter(description = "Channel id", example = "10") int channelId,
                                           @Parameter(description = "User id", example = "20") int userId);

    @Operation(summary = "Removes user from channel")
    ResponseEntity<HttpStatus> leaveFromChannel(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Returns channel's posts")
    ResponseEntity<List<ResponseChannelPostDTO>> getChannelPosts(
            @Parameter(description = "Channel id", example = "10") int id,
            @Parameter(description = "Page of posts", example = "0") int page,
            @Parameter(description = "Count of posts per page", example = "10") int count
    );

    @Operation(summary = "Returns channel's users")
    ResponseEntity<List<ResponseChannelUserDTO>> getChannelUsers(
            @Parameter(description = "Channel id", example = "10") int id,
            @Parameter(description = "Page of users", example = "0") int page,
            @Parameter(description = "Count of users per page", example = "10") int count
    );

    @Operation(summary = "Returns banned users in channel")
    ResponseEntity<List<ResponseUserDTO>> getBannedUsers(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Returns channel's logs")
    ResponseEntity<List<ResponseChannelLogDTO>> getChannelLogs(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Returns channel's options")
    ResponseEntity<ChannelOptionsDTO> getChannelOptions(@Parameter(description = "Channel id", example = "10") int id);

    @Operation(summary = "Updates channel's options")
    ResponseEntity<HttpStatus> updateChannelOptions(@Parameter(description = "Channel id", example = "10") int id,
                                                    ChannelOptionsDTO channelOptionsDTO);

    @Operation(summary = "Creates post in channel")
    @ApiResponse(description = "Post id")
    ResponseEntity<IdDTO> createPost(CreateChannelPostDTO createChannelPostDTO, BindingResult bindingResult);

    @Operation(summary = "Creates post with image in channel")
    @ApiResponse(description = "Post id")
    ResponseEntity<IdDTO> createImagePost(@Parameter(description = "Channel id", example = "10") int id,
                                          @RequestBody(content = {
                                                  @Content(mediaType = "image/jpeg"),
                                                  @Content(mediaType = "image/png")
                                          }) MultipartFile image);

    @Operation(summary = "Creates post with video in channel")
    @ApiResponse(description = "Post id")
    ResponseEntity<IdDTO> createVideoPost(@Parameter(description = "Channel id", example = "10") int id,
                                          @RequestBody(content = {
                                                  @Content(mediaType = "video/mp4")
                                          }) MultipartFile video);

    @Operation(summary = "Creates post with audio ogg in channel")
    @ApiResponse(description = "Post id")
    ResponseEntity<IdDTO> createAudioOggPost(@Parameter(description = "Channel id", example = "10") int id,
                                          @RequestBody(content = {
                                                  @Content(mediaType = "audio/ogg")
                                          }) MultipartFile audio);

    @Operation(summary = "Creates post with audio mp3 in channel")
    @ApiResponse(description = "Post id")
    ResponseEntity<IdDTO> createAudioMp3Post(@Parameter(description = "Channel id", example = "10") int id,
                                          @RequestBody(content = {
                                                  @Content(mediaType = "audio/mp3")
                                          }) MultipartFile audio);

    @Operation(summary = "Adds image to post")
    @ApiResponse(description = "File id")
    ResponseEntity<IdDTO> addImageToPost(@Parameter(description = "Post id", example = "20") long id,
                                         @RequestBody(content = {
                                                 @Content(mediaType = "image/jpeg"),
                                                 @Content(mediaType = "image/png")
                                         }) MultipartFile image);

    @Operation(summary = "Adds video to post")
    @ApiResponse(description = "File id")
    ResponseEntity<IdDTO> addVideoToPost(@Parameter(description = "Post id", example = "20") long id,
                                         @RequestBody(content = {
                                                 @Content(mediaType = "video/mp4")
                                         }) MultipartFile video);

    @Operation(summary = "Adds audio ogg to post")
    @ApiResponse(description = "File id")
    ResponseEntity<IdDTO> addAudioOggToPost(@Parameter(description = "Post id", example = "20") long id,
                                         @RequestBody(content = {
                                                 @Content(mediaType = "audio/ogg")
                                         }) MultipartFile audio);

    @Operation(summary = "Adds audio mp3 to post")
    @ApiResponse(description = "File id")
    ResponseEntity<IdDTO> addAudioMp3ToPost(@Parameter(description = "Post id", example = "20") long id,
                                         @RequestBody(content = {
                                                 @Content(mediaType = "audio/mp3")
                                         }) MultipartFile audio);

    @Operation(summary = "Returns post file")
    ResponseEntity<?> getPostFile(@Parameter(description = "File id", example = "30") long id);

    @Operation(summary = "Likes post in channel")
    ResponseEntity<HttpStatus> likePost(@Parameter(description = "Post id", example = "20") long id);

    @Operation(summary = "Deletes post's like")
    ResponseEntity<HttpStatus> deletePostLike(@Parameter(description = "Post id", example = "20") long id);

    @Operation(summary = "Returns post's comments")
    ResponseEntity<List<ResponseChannelPostCommentDTO>> getPostComments(
            @Parameter(description = "Post id", example = "20") long id,
            @Parameter(description = "Page of comments", example = "0") int page,
            @Parameter(description = "Count of comments per page", example = "10") int count
    );

    @Operation(summary = "Sends text comment to post")
    @ApiResponse(description = "Comment id")
    ResponseEntity<IdDTO> createPostComment(CreateChannelPostCommentDTO createDTO, BindingResult bindingResult);

    @Operation(summary = "Sends image comment to post")
    @ApiResponse(description = "Comment id")
    ResponseEntity<IdDTO> createPostImageComment(@Parameter(description = "Post id", example = "20") long id,
                                                 @RequestBody(content = {
                                                         @Content(mediaType = "image/jpeg"),
                                                         @Content(mediaType = "image/png")
                                                 }) MultipartFile image);

    @Operation(summary = "Sends video comment to post")
    @ApiResponse(description = "Comment id")
    ResponseEntity<IdDTO> createPostVideoComment(@Parameter(description = "Post id", example = "20") long id,
                                                 @RequestBody(content = {
                                                         @Content(mediaType = "video/mp4")
                                                 }) MultipartFile video);

    @Operation(summary = "Sends audio ogg comment to post")
    @ApiResponse(description = "Comment id")
    ResponseEntity<IdDTO> createPostAudioOggComment(@Parameter(description = "Post id", example = "20") long id,
                                                @RequestBody(content = {
                                                        @Content(mediaType = "audio/ogg")
                                                }) MultipartFile audio);

    @Operation(summary = "Sends audio mp3 comment to post")
    @ApiResponse(description = "Comment id")
    ResponseEntity<IdDTO> createPostAudioMP3Comment(@Parameter(description = "Post id", example = "20") long id,
                                                @RequestBody(content = {
                                                        @Content(mediaType = "audio/mp3")
                                                }) MultipartFile audio);

    @Operation(summary = "Returns comment's file")
    ResponseEntity<?> getCommentFile(@Parameter(description = "Comment id", example = "30") long id);

    @Operation(summary = "Updates post's comment")
    ResponseEntity<HttpStatus> updateComment(@Parameter(description = "Comment id", example = "30") long id,
                                             UpdateChannelPostCommentDTO updateChannelPostCommentDTO, BindingResult bindingResult);

    @Operation(summary = "Deletes comment")
    ResponseEntity<HttpStatus> deleteComment(@Parameter(description = "Comment id", example = "30") long id);

    @Operation(summary = "Deletes post")
    ResponseEntity<HttpStatus> deletePost(@Parameter(description = "Post id", example = "20") long id);

    @Operation(summary = "Updates post")
    ResponseEntity<HttpStatus> updatePost(UpdateChannelPostDTO updateChannelPostDTO, BindingResult bindingResult);


}