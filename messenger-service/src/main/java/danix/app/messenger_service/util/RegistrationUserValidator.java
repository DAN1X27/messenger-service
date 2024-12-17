package danix.app.messenger_service.util;

import danix.app.messenger_service.dto.RegistrationUserDTO;
import danix.app.messenger_service.repositories.UsersRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class RegistrationUserValidator implements Validator {

    private final UsersRepository usersRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return RegistrationUserDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegistrationUserDTO registrationUserDTO = (RegistrationUserDTO) target;
        usersRepository.findPersonByEmail(registrationUserDTO.getEmail()).ifPresent(person -> {
                    errors.rejectValue("email", "", "Email is busy");
                });
        usersRepository.findByUsername(registrationUserDTO.getUsername()).ifPresent(person -> {
                    errors.rejectValue("username", "", "Username is busy");
                });
    }
}
