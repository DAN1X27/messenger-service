package danix.app.messenger_service.util;

import danix.app.messenger_service.models.User;
import danix.app.messenger_service.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@AllArgsConstructor
public class PasswordValidator implements Validator {
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean supports(Class<?> clazz) {
        return String.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        String password = (String) target;
        User user = UserService.getCurrentUser();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            errors.rejectValue("password", "", "Invalid password");
        }
    }
}
