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

public interface GroupsAPI {

    @Operation(summary = "Returns all user's groups")
    ResponseEntity<List<ResponseGroupDTO>> getAll();

    @Operation(
            summary = "Returns all invites",
            description = "Returns all group invites sent to user"
    )
    ResponseEntity<List<ResponseGroupInviteDTO>> getInvites();

    @Operation(
            summary = "Returns group info",
            description = "Finds group by id and returns its info"
    )
    ResponseEntity<ShowGroupDTO> show(@Parameter(description = "Group id", example = "10") int id);

    @Operation(
            summary = "Returns group messages",
            description = "Finds group by id and returns its messages"
    )
    ResponseEntity<List<ResponseGroupMessageDTO>> getMessages(
            @Parameter(description = "Group id", example = "10") int id,
            @Parameter(description = "Page of messages", example = "0") int page,
            @Parameter(description = "Count of messages per page", example = "10") int count
    );

    @Operation(
            summary = "Returns group action messages",
            description = "Finds group by id and returns its action messages"
    )
    ResponseEntity<List<ResponseGroupActionMessageDTO>> getActionMessages(
            @Parameter(description = "Group id", example = "10") int id,
            @Parameter(description = "Page of messages", example = "0") int page,
            @Parameter(description = "Count of messages per page", example = "10") int count
    );

    @Operation(
            summary = "Returns group users",
            description = "Finds group by id and returns its users"
    )
    ResponseEntity<List<ResponseGroupUserDTO>> getUsers(
            @Parameter(description = "Group id", example = "10") int id,
            @Parameter(description = "Page of users", example = "0") int page,
            @Parameter(description = "Count of users per page", example = "10") int count
    );

    @Operation(summary = "Creates group")
    @ApiResponse(description = "Group id")
    ResponseEntity<IdDTO> createGroup(CreateGroupDTO createGroupDTO, BindingResult bindingResult);

    @Operation(
            summary = "Sends invite to group",
            description = "Finds group and user by id and sends invite to group to him"
    )
    @ApiResponse(description = "Invite id")
    ResponseEntity<IdDTO> invite(@Parameter(description = "Group id", example = "10") int groupId,
                                 @Parameter(description = "User id to send invite", example = "15") int userId);

    @Operation(
            summary = "Accepts invite to group",
            description = "Finds invite to group by id, accepts it and joins user to group"
    )
    ResponseEntity<HttpStatus> acceptInvite(@Parameter(description = "Invite id", example = "10") int id);

    @Operation(
            summary = "Bans user in group",
            description = "Finds group and user by id and bans user in group"
    )
    ResponseEntity<HttpStatus> banUser(@Parameter(description = "Group id", example = "10") int groupId,
                                       @Parameter(description = "User id", example = "15") int userId);

    @Operation(
            summary = "Unbans user in group",
            description = "Finds group and user by id and unbans user in group"
    )
    ResponseEntity<HttpStatus> unbanUser(@Parameter(description = "Group id", example = "10") int groupId,
                                         @Parameter(description = "User id", example = "15") int userId);

    @Operation(
            summary = "Kicks user from group",
            description = "Finds group and user by id and kicks user from group"
    )
    ResponseEntity<HttpStatus> kickUser(@Parameter(description = "Group id", example = "10") int groupId,
                                        @Parameter(description = "User id", example = "15") int userId);

    @Operation(
            summary = "Removes current user from group",
            description = "Finds group by id and removes current user from it"
    )
    ResponseEntity<HttpStatus> leaveFromGroup(@Parameter(description = "Group id", example = "10") int groupId);

    @Operation(
            summary = "Returns group image",
            description = "Finds group by id and returns its image"
    )
    ResponseEntity<?> getImage(@Parameter(description = "Group id", example = "10") int id);

    @Operation(
            summary = "Deletes group image",
            description = "Finds group by id and deletes its image"
    )
    ResponseEntity<HttpStatus> deleteImage(@Parameter(description = "Group id", example = "10") int id);

    @Operation(
            summary = "Updates group image",
            description = "Finds group by id and updates its image"
    )
    ResponseEntity<HttpStatus> updateImage(@Parameter(description = "Group id", example = "10") int id,
                                           @RequestBody(description = "Image", content = {
                                                   @Content(mediaType = "image/jpeg"),
                                                   @Content(mediaType = "image/png")
                                           }) MultipartFile image);

    @Operation(
            summary = "Deletes group",
            description = "Finds group by id and deletes it"
    )
    ResponseEntity<HttpStatus> deleteGroup(@Parameter(description = "Group id", example = "10") int id);

    @Operation(
            summary = "Updates group",
            description = "Finds group by id and updates it"
    )
    ResponseEntity<HttpStatus> updateGroup(UpdateGroupDTO updateGroupDTO, BindingResult bindingResult);

    @Operation(
            summary = "Adds admin to group",
            description = "Finds group and user by id and sets user to administrator in group"
    )
    ResponseEntity<HttpStatus> addAdmin(@Parameter(description = "Group id", example = "10") int groupId,
                                        @Parameter(description = "User id", example = "15") int userId);

    @Operation(
            summary = "Deletes admin in group",
            description = "Finds group by id and admin by user id and sets admin to user"
    )
    ResponseEntity<HttpStatus> deleteAdmin(@Parameter(description = "Group id", example = "10") int groupId,
                                           @Parameter(description = "User id", example = "15") int userId);

    @Operation(
            summary = "Sends message to group",
            description = "Finds group by id and sends message to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendMessage(@Parameter(description = "Group id", example = "10") int id, MessageDTO messageDTO,
                                      BindingResult bindingResult);

    @Operation(
            summary = "Sends image to group",
            description = "Finds group by id and sends image to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendImage(@Parameter(description = "Group id", example = "10") int id,
                                    @RequestBody(description = "Image", content = {
                                            @Content(mediaType = "image/jpeg"),
                                            @Content(mediaType = "image/png")
                                    }) MultipartFile image);

    @Operation(
            summary = "Sends video to group",
            description = "Finds group by id and sends video to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendVideo(@Parameter(description = "Group id", example = "10") int id,
                                    @RequestBody(description = "Video", content = {
                                            @Content(mediaType = "video/mp4")
                                    }) MultipartFile video);

    @Operation(
            summary = "Sends audio ogg to group",
            description = "Finds group by id and sends audio ogg to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendAudioOgg(@Parameter(description = "Group id", example = "10") int id,
                                       @RequestBody(description = "Audio", content = {
                                               @Content(mediaType = "audio/ogg")
                                       }) MultipartFile audio);

    @Operation(
            summary = "Sends audio mp3 to group",
            description = "Finds group by id and sends audio mp3 to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendAudioMP3(@Parameter(description = "Group id", example = "10") int id,
                                       @RequestBody(description = "Audio", content = {
                                               @Content(mediaType = "audio/mp3")
                                       }) MultipartFile audio);

    @Operation(
            summary = "Returns message file",
            description = "Finds message by id and returns its file"
    )
    ResponseEntity<?> getMessageFile(@Parameter(description = "Message id", example = "20") long id);

    @Operation(
            summary = "Updates message",
            description = "Finds message by id and updates it"
    )
    ResponseEntity<HttpStatus> updateMessage(@Parameter(description = "Message id", example = "20") long id,
                                             MessageDTO messageDTO, BindingResult bindingResult);

    @Operation(
            summary = "Deletes message",
            description = "Finds message by id and deletes it"
    )
    ResponseEntity<HttpStatus> deleteMessage(@Parameter(description = "Message id", example = "20") long id);
}
