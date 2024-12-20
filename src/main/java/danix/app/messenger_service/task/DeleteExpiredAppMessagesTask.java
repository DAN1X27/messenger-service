package danix.app.messenger_service.task;

import danix.app.messenger_service.repositories.AppMessagesRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@AllArgsConstructor
public class DeleteExpiredAppMessagesTask {
    private final AppMessagesRepository appMessagesRepository;
    private static final Logger logger = LoggerFactory.getLogger(DeleteExpiredAppMessagesTask.class);

    @Scheduled(cron = "@midnight")
    @Transactional
    public void run() {
        logger.info("Start deleting expired app messages");
        appMessagesRepository.deleteExpiredMessages();
        logger.info("Finished deleting expired app messages");
    }
}
