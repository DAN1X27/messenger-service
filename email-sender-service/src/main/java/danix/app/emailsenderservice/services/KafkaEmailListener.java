package danix.app.emailsenderservice.services;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@EnableKafka
public class KafkaEmailListener {
    private final EmailSenderServiceImpl emailSenderServiceImpl;

    @Autowired
    public KafkaEmailListener(EmailSenderServiceImpl emailSenderServiceImpl) {
        this.emailSenderServiceImpl = emailSenderServiceImpl;
    }

    @KafkaListener(topics = "registration-topic")
    public void registrationEmailListener(ConsumerRecord<String, String> record) {
        emailSenderServiceImpl.sendMessage(
                record.key(),
                "Thank you for registration on our service - " + record.value()
        );
    }

    @KafkaListener(topics = "ban_user-topic")
    public void sendBanUserMessage(ConsumerRecord<String, String> record) {
        emailSenderServiceImpl.sendMessage(
                record.key(),
                "Your account with email:" + record.key() + " - has been banned for reason: " + record.value()
        );
    }

    @KafkaListener(topics = "unban_user-topic")
    public void sendUnbanUserMessage(String email) {
        emailSenderServiceImpl.sendMessage(
                email,
                "Your account with email: " + email + " - has been unbanned!"
        );
    }

    @KafkaListener(topics = "registration_key-topic")
    public void sendRegistrationKey(ConsumerRecord<String, String> record) {
        emailSenderServiceImpl.sendMessage(
                record.key(),
                "Your registration key, do not show it to anyone!: " + record.value()
        );
    }

        @KafkaListener(topics = "recover_password-topic")
    public void sendRecoverPasswordKey(ConsumerRecord<String, String> record) {
        emailSenderServiceImpl.sendMessage(
                record.key(),
                "Your key for recover password: " + record.value()
        );
    }

    @KafkaListener(topics = "ban_channel-topic")
    public void banChannelMessage(ConsumerRecord<String, String> record) {
        emailSenderServiceImpl.sendMessage(
                record.key(),
                record.value()
        );
    }

    @KafkaListener(topics = "unban_channel-topic")
    public void unbanChannelMessage(ConsumerRecord<String, String> record) {
        emailSenderServiceImpl.sendMessage(
                record.key(),
                record.value()
        );
    }
}
