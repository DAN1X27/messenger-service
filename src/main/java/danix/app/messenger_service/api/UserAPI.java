package danix.app.messenger_service.api;

import danix.app.messenger_service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserAPI {

    @Operation(summary = "Returns user's info")
    ResponseEntity<UserInfoDTO> getInfo();

    @Operation(summary = "Returns user's image")
    ResponseEntity<?> getImage();

    @Operation(
            summary = "Returns user's image",
            description = "Finds user by id and returns his image"
    )
    ResponseEntity<?> getImage(@Parameter(description = "User ID", example = "10") int id);

    @Operation(summary = "Updates user's image")
    ResponseEntity<HttpStatus> updateImage(@RequestBody(description = "New user's image in form data",
            content = {@Content(mediaType = "image/jpeg"), @Content(mediaType = "image/png")}) MultipartFile image);

    @Operation(summary = "Delete user's image")
    ResponseEntity<HttpStatus> deleteImage();

    @Operation(
            summary = "Finds and returns user's info",
            description = "Finds user by username and returns his info"
    )
    ResponseEntity<ShowUserDTO> find(@Parameter(description = "Search username", example = "user1") String username);

    @Operation(summary = "Returns user's notifications")
    ResponseEntity<List<ResponseAppMessageDTO>> getNotifications();

    @Operation(summary = "Returns user's friend requests")
    ResponseEntity<List<ResponseUserDTO>> getFriendRequests();

    @Operation(summary = "Returns user's friends")
    ResponseEntity<List<ResponseUserDTO>> getFriends();

    @Operation(
            summary = "Blocks user",
            description = "Finds user by id and blocks"
    )
    ResponseEntity<HttpStatus> blockUser(@Parameter(description = "User ID", example = "10") int id);

    @Operation(
            summary = "Unblocks user",
            description = "Finds user by id and unblocks"
    )
    ResponseEntity<HttpStatus> unblockUser(@Parameter(description = "USER ID", example = "10") int id);

    @Operation(
            summary = "Sends friend request",
            description = "Find user by username and send friend request"
    )
    ResponseEntity<HttpStatus> addFriend(@Parameter(description = "Search username", example = "user1") String username);

    @Operation(
            summary = "Accepts friend request",
            description = "Finds friend request by id and accepts"
    )
    ResponseEntity<HttpStatus> acceptFriendRequest(@Parameter(description = "USER ID", example = "10") int id);

    @Operation(
            summary = "Cancels friend request",
            description = "Finds friend request by id and cancel"
    )
    ResponseEntity<HttpStatus> cancelFriendRequest(@Parameter(description = "Friend request id", example = "10") int id);

    @Operation(
            summary = "Reject friend request",
            description = "Finds friend request by id and reject"
    )
    ResponseEntity<HttpStatus> rejectFriendRequest(@Parameter(description = "Friend request id", example = "10") int id);

    @Operation(
            summary = "Delete friend",
            description = "Finds friend by user id and deletes"
    )
    ResponseEntity<HttpStatus> deleteFriend(@Parameter(description = "User ID", example = "10") int id);

    @Operation(summary = "Updates user's info")
    ResponseEntity<HttpStatus> updateInfo(UpdateUserDTO updateUserDTO, BindingResult bindingResult);

    @Operation(summary = "Updates user's password")
    ResponseEntity<HttpStatus> updatePassword(UpdatePasswordDTO updatePasswordDTO, BindingResult bindingResult);

}
