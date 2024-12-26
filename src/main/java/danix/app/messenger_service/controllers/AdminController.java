package danix.app.messenger_service.controllers;

import danix.app.messenger_service.models.User;
import danix.app.messenger_service.services.ChannelsService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PatchMapping("/user/ban/{id}")
    public ResponseEntity<HttpStatus> banUser(@PathVariable int id, @RequestBody Map<String, String> reasonData) {
        String reason = reasonData.get("reason");
        if (reason == null || reason.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        User user = userService.getById(id);
        userService.banUser(id, reason);
        kafkaTemplate.send("ban_user-topic", user.getEmail(), reason);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/user/unban/{id}")
    public ResponseEntity<HttpStatus> unBanUser(@PathVariable int id) {
        User person = userService.getById(id);
        userService.unBanUser(id);
        kafkaTemplate.send("unban_user-topic", person.getEmail());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/channel/ban/{id}")
    public ResponseEntity<HttpStatus> banChannel(@PathVariable int id, @RequestBody Map<String, String> reasonData) {
        String reason = reasonData.get("reason");
        if (reason == null || reason.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        channelsService.banChannel(id, reason);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/channel/unban/{id}")
    public ResponseEntity<HttpStatus> unbanChannel(@PathVariable int id) {
        channelsService.unbanChannel(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
