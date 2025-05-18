package danix.app.messenger_service.api;

import danix.app.messenger_service.dto.ReasonDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public interface AdminAPI {

    @Operation(
            summary = "Bans user",
            description = "Finds user by id and bans him"
    )
    ResponseEntity<HttpStatus> banUser(@Parameter(description = "User id", example = "10") int id, ReasonDTO reasonDTO);

    @Operation(
            summary = "Unbans user",
            description = "Finds user by id and unbans him"
    )
    ResponseEntity<HttpStatus> unbanUser(@Parameter(description = "User id", example = "10") int id);

    @Operation(
            summary = "Bans channel",
            description = "Finds chanel by id and bans it"
    )
    ResponseEntity<HttpStatus> banChannel(@Parameter(description = "Channel id", example = "10") int id, ReasonDTO reasonDTO);

    @Operation(
            summary = "Unbans channel",
            description = "Finds channel by id and unbans it"
    )
    ResponseEntity<HttpStatus> unbanChannel(@Parameter(description = "Channel id", example = "10") int id);

}
