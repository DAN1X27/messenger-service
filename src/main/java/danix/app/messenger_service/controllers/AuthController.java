package danix.app.messenger_service.controllers;

import danix.app.messenger_service.dto.AcceptRequestEmailKeyDTO;
import danix.app.messenger_service.dto.AuthDTO;
import danix.app.messenger_service.dto.RegistrationUserDTO;
import danix.app.messenger_service.dto.RecoverPasswordDTO;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.BannedUsersRepository;
import danix.app.messenger_service.repositories.EmailsKeysRepository;
import danix.app.messenger_service.security.JWTUtil;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.services.TokensService;
import danix.app.messenger_service.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationProvider authenticationProvider;
    private final JWTUtil jwtUtil;
    private final TokensService tokensService;
    private final RegistrationUserValidator registrationUserValidator;
    private final BannedUsersRepository bannedUsersRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailsKeysRepository emailsKeysRepository;
    private final EmailKeyValidator emailKeyValidator;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthDTO authDTO) {
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
        return ResponseEntity.ok(Map.of("jwt-token", jwtToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<HttpStatus> logout() {
        User currentUser = UserService.getCurrentUser();
        tokensService.getAllUserTokens(currentUser).forEach(token -> tokensService.updateStatus(token.getId(), TokenStatus.REVOKED));
        return ResponseEntity.ok(HttpStatus.OK);
    }

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

    @PatchMapping("/registration/accept")
    public ResponseEntity<HttpStatus> acceptRegistrationKey(@RequestBody @Valid AcceptRequestEmailKeyDTO acceptEmailDTO,
                                                        BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        emailKeyValidator.validate(acceptEmailDTO, bindingResult);
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        User user = userService.getByEmail(acceptEmailDTO.getEmail());
        userService.registerUser(acceptEmailDTO.getEmail());
        kafkaTemplate.send("registration-topic", acceptEmailDTO.getEmail(), user.getUsername());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/password")
    public ResponseEntity<HttpStatus> forgotPassword(@RequestBody Map<String, String> map) {

        String email = map.get("email");
        if (email == null) {
            throw new AuthenticationException("Invalid email");
        }
        userService.getByEmail(email);
        emailsKeysRepository.findByEmail(email).ifPresent(key -> {
            if (key.getExpiredTime().isAfter(LocalDateTime.now())) {
                throw new AuthenticationException("User already have an active key");
            }
            userService.deleteEmailKey(key);
        });
        userService.sendRecoverPasswordKey(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/password")
    public ResponseEntity<HttpStatus> recoverPassword(@RequestBody @Valid RecoverPasswordDTO recoverPasswordDTO,
                                                 BindingResult bindingResult) {
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        emailKeyValidator.validate(recoverPasswordDTO, bindingResult);
        ErrorHandler.handleException(bindingResult, ExceptionType.AUTHENTICATION_EXCEPTION);
        User user = userService.getByEmail(recoverPasswordDTO.getEmail());
        user.setPassword(passwordEncoder.encode(recoverPasswordDTO.getNewPassword()));
        userService.updateUser(user.getId(), user);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AbstractException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
