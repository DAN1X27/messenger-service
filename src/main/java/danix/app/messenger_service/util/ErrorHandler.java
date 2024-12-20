package danix.app.messenger_service.util;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

public class ErrorHandler {
    public static void handleException(BindingResult bindingResult, ExceptionType exceptionType) {
        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            StringBuilder message = new StringBuilder();
            for (FieldError fieldError : fieldErrors) {
                message.append(fieldError.getField()).append("-")
                        .append(fieldError.getDefaultMessage()).append("; ");
            }

            switch (exceptionType) {
                case USER_EXCEPTION ->
                    throw new UserException(message.toString());
                case MESSAGE_EXCEPTION ->
                    throw new MessageException(message.toString());
                case CHAT_EXCEPTION ->
                    throw new ChatException(message.toString());
                case GROUP_EXCEPTION ->
                    throw new GroupException(message.toString());
                case CHANNEL_EXCEPTION ->
                    throw new ChannelException(message.toString());
                case AUTHENTICATION_EXCEPTION ->
                    throw new AuthenticationException(message.toString());
            }
        }
    }
}