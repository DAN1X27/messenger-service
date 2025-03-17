package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.User;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UsersController {
    private final UserService userService;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;

    @MessageMapping("/status/online")
    public ResponseEntity<HttpStatus> updateOnlineStatus() {
        userService.setOnlineStatus();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @MessageMapping("/status/offline")
    public ResponseEntity<HttpStatus> setOfflineStatus() {
        userService.setOfflineStatus();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoDTO> showUserInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    @GetMapping("/image")
    public ResponseEntity<?> showUserImage() {
        ResponseFileDTO image = userService.getImage(UserService.getCurrentUser().getId());
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getFileData());
    }

    @GetMapping("/find")
    public ResponseEntity<ShowUserDTO> findUser(@RequestParam String username) {
        ShowUserDTO user = userService.findUser(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> findUserImage(@PathVariable int id) {
        ResponseFileDTO image = userService.getImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getFileData());
    }

    @GetMapping("/notifications")
    public List<ResponseAppMessageDTO> showUserNotifications() {
        return userService.getAppMessages();
    }

    @GetMapping("/friends/requests")
    public List<ResponseUserDTO> showFriendsRequests() {
        return userService.getAllFriendRequests();
    }

    @GetMapping("/friends")
    public List<ShowUserDTO> getUserFriends() {
        return userService.getAllUserFriends();
    }

    @PostMapping("/block/{id}")
    public ResponseEntity<HttpStatus> blockUser(@PathVariable int id) {
        userService.blockUser(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/friend")
    public ResponseEntity<HttpStatus> addFriend(@RequestParam String username) {
        userService.addFriend(username);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/image")
    public ResponseEntity<HttpStatus> updateImage(@RequestParam("image") MultipartFile image) {
        userService.addImage(image, UserService.getCurrentUser().getId());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/friend/request/{id}")
    public ResponseEntity<HttpStatus> acceptFriendRequest(@PathVariable int id) {
        userService.acceptFriend(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping
    public ResponseEntity<HttpStatus> updateInfo(@RequestBody @Valid UpdateUserDTO updateUserDTO,
                                                  BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.USER_EXCEPTION);
        userService.update(updateUserDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/password")
    public ResponseEntity<HttpStatus> updatePassword(@RequestBody @Valid UpdatePasswordDTO updatePasswordDTO,
                                                     BindingResult bindingResult) {
        passwordValidator.validate(updatePasswordDTO.getPassword(), bindingResult);
        ErrorHandler.handleException(bindingResult, ExceptionType.USER_EXCEPTION);
        User currentUser = getCurrentUser();
        if (passwordEncoder.matches(updatePasswordDTO.getNewPassword(), currentUser.getPassword())) {
            throw new UserException("Password must be different from the old one");
        }
        userService.updatePassword(updatePasswordDTO.getNewPassword());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/unblock/{id}")
    public ResponseEntity<HttpStatus> unblockUser(@PathVariable int id) {
        userService.unblockUser(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/image")
    public ResponseEntity<HttpStatus> deleteImage() {
        userService.deleteImage(UserService.getCurrentUser().getId());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/friend/request/cancel/{id}")
    public ResponseEntity<HttpStatus> cancelFriendRequest(@PathVariable int id) {
        userService.cancelFriendRequest(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/friend/request/reject/{id}")
    public ResponseEntity<HttpStatus> rejectFriendRequest(@PathVariable int id) {
        userService.rejectFriendRequest(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/friend/{id}")
    public ResponseEntity<HttpStatus> deleteFriend(@PathVariable int id) {
        userService.deleteFriend(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}