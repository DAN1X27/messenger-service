package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.User;
import danix.app.messenger_service.repositories.UsersRepository;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UsersController {
    private final UserService userService;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/info")
    public ResponseEntity<UserInfoDTO> showUserInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    @GetMapping("/image")
    public ResponseEntity<?> showUserImage() {
        ResponseImageDTO image = userService.getImage(UserService.getCurrentUser().getId());
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getImageData());
    }

    @PatchMapping("/status")
    public ResponseEntity<HttpStatus> updateOnlineStatus() {
        userService.updateOnlineStatus();
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/notifications")
    public List<ResponseAppMessageDTO> showUserNotifications() {
        return userService.getAppMessages();
    }

    @PatchMapping("/image")
    public ResponseEntity<HttpStatus> updateImage(@RequestParam("image") MultipartFile image) {
        userService.addImage(image, UserService.getCurrentUser().getId());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/image")
    public ResponseEntity<HttpStatus> deleteImage() {
        userService.deleteImage(UserService.getCurrentUser().getId());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/find")
    public ResponseEntity<ShowUserDTO> findUser(@RequestBody Map<String, String> userData) {
        requestHelper(userData);
        ShowUserDTO user = userService.findUser(userData.get("username"));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> findUserImage(@PathVariable int id) {
        ResponseImageDTO image = userService.getImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(image.getType())
                .body(image.getImageData());
    }

    @GetMapping("/friends/requests")
    public List<ResponseUserDTO> showFriendsRequests() {
        return userService.getAllFriendsRequests();
    }

    @DeleteMapping("/unblock/{id}")
    public ResponseEntity<HttpStatus> unblockUser(@PathVariable int id) {
        userService.unblockUser(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/block/{id}")
    public ResponseEntity<HttpStatus> blockUser(@PathVariable int id) {
        userService.blockUser(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/friends/request/{id}")
    public ResponseEntity<HttpStatus> cancelFriendRequest(@PathVariable int id) {
        userService.cancelFriendRequest(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/friends/request/{id}")
    public ResponseEntity<HttpStatus> acceptFriendRequest(@PathVariable int id) {
        userService.acceptFriend(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/username")
    public ResponseEntity<HttpStatus> updateUsername(@RequestBody @Valid UpdateUsernameDTO updateUsernameDTO,
                                                     BindingResult bindingResult) {
        passwordValidator.validate(updateUsernameDTO.getPassword(), bindingResult);
        ErrorHandler.handleException(bindingResult, ExceptionType.USER_EXCEPTION);
        User currentUser = getCurrentUser();
        if (updateUsernameDTO.getUsername().equals(currentUser.getUsername())) {
            throw new UserException("Еhe new username must be different from the old one");
        }
        currentUser.setUsername(updateUsernameDTO.getUsername());
        userService.updateUser(currentUser.getId(), currentUser);
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
        currentUser.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        userService.updateUser(currentUser.getId(), currentUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/friends")
    public List<ShowUserDTO> getUserFriends() {
        return userService.getAllUserFriends();
    }

    @DeleteMapping("/friends")
    public ResponseEntity<HttpStatus> deleteFriend(@RequestBody Map<String, String> userData) {
        requestHelper(userData);
        String username = userData.get("username");
        userService.deleteFriend(username);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/friends")
    public ResponseEntity<HttpStatus> addFriend(@RequestBody Map<String, String> userData) {
        requestHelper(userData);
        String username = userData.get("username");
        userService.addFriend(username);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    private void requestHelper(Map<String, String> userData) {
        if (userData.get("username") == null || userData.get("username").isEmpty()) {
            throw new UserException("Invalid username");
        }
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
