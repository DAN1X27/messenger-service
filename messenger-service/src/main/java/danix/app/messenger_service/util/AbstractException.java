package danix.app.messenger_service.util;

public abstract class AbstractException extends RuntimeException{
    public AbstractException(String message) {
        super(message);
    }
}
