package danix.app.messenger_service.controllers;

import danix.app.messenger_service.api.AuthenticationAPI;
import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BannedUsersRepository;
import danix.app.messenger_service.repositories.EmailsKeysRepository;
import danix.app.messenger_service.security.JWTUtil;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.services.TokensService;
import danix.app.messenger_service.util.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController implements AuthenticationAPI {
    private final UserService userService;
    private final AuthenticationProvider authenticationProvider;
    private final JWTUtil jwtUtil;
    private final TokensService tokensService;
    private final RegistrationUserValidator registrationUserValidator;
    private final BannedUsersRepository bannedUsersRepository;
    private final EmailsKeysRepository emailsKeysRepository;
    private final EmailKeyValidator emailKeyValidator;

    @Override
    @PostMapping("/login")
    public ResponseEntity<ResponseJWTTokenDTO> login(@RequestBody @Valid AuthDTO authDTO,
                                                     BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        User user = userService.getByEmail(authDTO.getEmail());
        bannedUsersRepository.findByUser(user).ifPresent(bannedUser -> {
            throw new AuthenticationException("Account has been banned for reason: " +
                    bannedUser.getReason());
        });
        if (user.getUserStatus() == User.Status.TEMPORALLY_REGISTERED) {
            throw new AuthenticationException("User not found");
        }
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword());
        try {
            authenticationProvider.authenticate(authenticationToken);
        }catch (BadCredentialsException e) {
            throw new AuthenticationException("Incorrect password");
        }
        String jwtToken = jwtUtil.generateToken(authDTO.getEmail());
        tokensService.create(jwtToken, userService.getByEmail(authDTO.getEmail()));
        return new ResponseEntity<>(new ResponseJWTTokenDTO(jwtToken), HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<HttpStatus> logout() {
        User currentUser = UserService.getCurrentUser();
        tokensService.banUserTokens(currentUser.getId());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Override
    @PostMapping("/registration")
    public ResponseEntity<HttpStatus> registration(@RequestBody @Valid RegistrationUserDTO registrationUserDTO,
                                               BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        registrationUserValidator.validate(registrationUserDTO, bindingResult);
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        userService.deleteTempUser(registrationUserDTO.getEmail());
        userService.temporalRegister(registrationUserDTO);
        userService.sendRegistrationKey(registrationUserDTO.getEmail());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/password")
    public ResponseEntity<HttpStatus> sendRecoverPasswordKey(@RequestParam String email) {
        userService.getByEmail(email);
        emailsKeysRepository.findByEmail(email).ifPresent(key -> {
            if (key.getExpiredTime().isAfter(LocalDateTime.now())) {
                throw new AuthenticationException("User already have an active key");
            }
            userService.deleteEmailKey(key);
        });
        userService.sendRecoverPasswordKey(email);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    @PatchMapping("/registration/accept")
    public ResponseEntity<ResponseJWTTokenDTO> acceptRegistration(@RequestBody @Valid AcceptRequestEmailKeyDTO acceptEmailDTO,
                                                        BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        emailKeyValidator.validate(acceptEmailDTO, bindingResult);
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        userService.registerUser(acceptEmailDTO.getEmail());
        String jwtToken = jwtUtil.generateToken(acceptEmailDTO.getEmail());
        tokensService.create(jwtToken, userService.getByEmail(acceptEmailDTO.getEmail()));
        return new ResponseEntity<>(new ResponseJWTTokenDTO(jwtToken), HttpStatus.CREATED);
    }

    @Override
    @PatchMapping("/password")
    public ResponseEntity<HttpStatus> recoverPassword(@RequestBody @Valid RecoverPasswordDTO recoverPasswordDTO,
                                                 BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        emailKeyValidator.validate(recoverPasswordDTO, bindingResult);
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        userService.updatePassword(recoverPasswordDTO.getNewPassword());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
