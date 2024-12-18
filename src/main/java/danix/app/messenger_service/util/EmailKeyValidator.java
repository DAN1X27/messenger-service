package danix.app.messenger_service.util;

import danix.app.messenger_service.dto.RequestEmailKey;
import danix.app.messenger_service.repositories.EmailsKeysRepository;
import danix.app.messenger_service.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmailKeyValidator implements Validator {
    private final UserService userService;
    private final EmailsKeysRepository emailsKeysRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return RequestEmailKey.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RequestEmailKey key = (RequestEmailKey) target;

        emailsKeysRepository.findByEmail(key.getEmail()).ifPresentOrElse(emailKey -> {
            if (emailKey.getKey() != key.getKey()) {
                userService.updateEmailKeyAttempts(emailKey);
                if (emailKey.getAttempts() >= 3) {
                    userService.deleteEmailKey(emailKey);
                    userService.deleteTempUser(key.getEmail());
                    errors.rejectValue("key", "", "The limit of attempts has been exceeded, send the key again");
                } else {
                    errors.rejectValue("key", "", "Invalid key");
                }
            } else if (emailKey.getExpiredTime().isBefore(LocalDateTime.now())) {
                userService.deleteEmailKey(emailKey);
                userService.deleteTempUser(key.getEmail());
                errors.rejectValue("key", "", "Expired key");
            } else {
                userService.deleteEmailKey(emailKey);
            }
        }, () -> errors.rejectValue("email", "", "Email not found"));
    }
}
