package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.BanChannelDTO;
import danix.app.messenger_service.dto.BanUserDTO;
import danix.app.messenger_service.models.User;
import danix.app.messenger_service.services.ChannelsService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChannelsService channelsService;

    @PatchMapping("/users/ban")
    public ResponseEntity<HttpStatus> banUser(@RequestBody @Valid BanUserDTO banUserDTO) {
        User user = userService.getByUsername(banUserDTO.getUsername());
        userService.banUser(banUserDTO);
        kafkaTemplate.send("ban_user-topic", user.getEmail(), banUserDTO.getReason());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/users/unban")
    public ResponseEntity<HttpStatus> unBanUser(@RequestBody Map<String ,String> user) {
        if (user.get("username") == null) {
            throw new UserException("Incorrect key");
        }
        User person = userService.getByUsername(user.get("username"));
        userService.unBanUser(user.get("username"));
        kafkaTemplate.send("unban_user-topic", person.getEmail());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/channels/ban")
    public ResponseEntity<HttpStatus> banChannel(@RequestBody @Valid BanChannelDTO banChannelDTO,
                                                 BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.CHANNEL_EXCEPTION);
        channelsService.banChannel(banChannelDTO);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/channels/unban/{id}")
    public ResponseEntity<HttpStatus> unbanChannel(@PathVariable int id) {
        channelsService.unbanChannel(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(UserException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(ChannelException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
