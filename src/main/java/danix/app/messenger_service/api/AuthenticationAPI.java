package danix.app.messenger_service.api;

import danix.app.messenger_service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface AuthenticationAPI {

    @Operation(
            summary = "Login user",
            description = "Logins user and returns JWT token"
    )
    ResponseEntity<ResponseJWTTokenDTO> login(AuthDTO authDTO, BindingResult bindingResult);

    @Operation(summary = "Logout user")
    ResponseEntity<HttpStatus> logout();

    @Operation(
            summary = "Temporally registers user",
            description = "Temporally registers user and sends registration code to users email"
    )
    ResponseEntity<HttpStatus> registration(RegistrationUserDTO registrationUserDTO, BindingResult bindingResult);

    @Operation(
            summary = "Registers user",
            description = "Validate email code, registers user and returns JWT token"
    )
    ResponseEntity<ResponseJWTTokenDTO> acceptRegistration(AcceptRequestEmailKeyDTO emailKeyDTO, BindingResult bindingResult);

    @Operation(summary = "Sends code for recover password to users email")
    ResponseEntity<HttpStatus> sendRecoverPasswordKey(String email);

    @Operation(
            summary = "Recovers password",
            description = "Validate email code and update users password"
    )
    ResponseEntity<HttpStatus> recoverPassword(RecoverPasswordDTO recoverPasswordDTO, BindingResult bindingResult);

}
