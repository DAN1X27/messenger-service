package danix.app.messenger_service.controllers;

import danix.app.messenger_service.api.AdminAPI;
import danix.app.messenger_service.dto.ReasonDTO;
import danix.app.messenger_service.services.ChannelsService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin API")
public class AdminController implements AdminAPI {
    private final UserService userService;
    private final ChannelsService channelsService;

    @Override
    @PatchMapping("/user/ban/{id}")
    public ResponseEntity<HttpStatus> banUser(@PathVariable int id, @RequestBody ReasonDTO reasonData) {
        userService.banUser(id, reasonData.reason());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PatchMapping("/user/unban/{id}")
    public ResponseEntity<HttpStatus> unbanUser(@PathVariable int id) {
        userService.unbanUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PatchMapping("/channel/ban/{id}")
    public ResponseEntity<HttpStatus> banChannel(@PathVariable int id, @RequestBody ReasonDTO reasonDTO) {
        channelsService.banChannel(id, reasonDTO.reason());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PatchMapping("/channel/unban/{id}")
    public ResponseEntity<HttpStatus> unbanChannel(@PathVariable int id) {
        channelsService.unbanChannel(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
