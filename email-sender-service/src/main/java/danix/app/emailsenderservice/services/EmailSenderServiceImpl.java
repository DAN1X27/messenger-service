package danix.app.emailsenderservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {

    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailSenderServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendMessage(String to, String message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setText(message);
        msg.setFrom("danikcheban2007@gmail.com");
        msg.setSubject("Spring messenger service application");
        javaMailSender.send(msg);
    }
}
