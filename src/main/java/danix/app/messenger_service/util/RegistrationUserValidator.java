package danix.app.messenger_service.util;

import danix.app.messenger_service.dto.RegistrationUserDTO;
import danix.app.messenger_service.repositories.EmailsKeysRepository;
import danix.app.messenger_service.repositories.UsersRepository;
import danix.app.messenger_service.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class RegistrationUserValidator implements Validator {
    private final EmailsKeysRepository emailsKeysRepository;
    private final UsersRepository usersRepository;
    private final UserService userService;

    @Override
    public boolean supports(Class<?> clazz) {
        return RegistrationUserDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegistrationUserDTO registrationUserDTO = (RegistrationUserDTO) target;
        emailsKeysRepository.findByEmail(registrationUserDTO.getEmail()).ifPresentOrElse(key -> {
            if (key.getExpiredTime().isAfter(LocalDateTime.now())) {
                errors.rejectValue("email", "", "User already have an active key");
            } else {
                userService.deleteEmailKey(key);
                userService.deleteTempUser(registrationUserDTO.getEmail());
            }
        }, () -> {
            usersRepository.findByEmail(registrationUserDTO.getEmail())
                    .ifPresent(person -> errors.rejectValue("email", "", "Email is busy"));
            usersRepository.findByUsername(registrationUserDTO.getUsername())
                    .ifPresent(person -> errors.rejectValue("username", "", "Username is busy"));
        });
    }
}
