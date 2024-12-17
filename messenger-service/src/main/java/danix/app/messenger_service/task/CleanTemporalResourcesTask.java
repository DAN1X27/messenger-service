package danix.app.messenger_service.task;
import danix.app.messenger_service.models.User;
import danix.app.messenger_service.repositories.EmailsKeysRepository;
import danix.app.messenger_service.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
public class CleanTemporalResourcesTask {
    private final EmailsKeysRepository emailsKeysRepository;
    private final UsersRepository usersRepository;

    @Transactional
    @Scheduled(cron = "@hourly")
    public void deleteTemporalUsers() {
        log.info("Start deleting temporal users");
        usersRepository.deleteAllByUserStatusAndCreatedAtBefore(User.Status.TEMPORALLY_REGISTERED,
                LocalDateTime.now().minusDays(1));
        log.info("Finish deleting temporal users");
    }

    @Transactional
    @Scheduled(cron = "@hourly")
    public void deleteExpiredRegistrationKeys() {
        log.info("Start deleting expired registration keys");
        emailsKeysRepository.deleteExpiredKeys();
        log.info("Finish deleting expired registration keys");
    }
}
