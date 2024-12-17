package danix.app.emailsenderservice.services;

public interface EmailSenderService {
    void sendMessage(String to, String text);
}
