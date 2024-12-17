package danix.app.messenger_service.task;

import danix.app.messenger_service.repositories.ChannelsLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteExpiredLogsTask {
    private final ChannelsLogsRepository channelsLogsRepository;

    @Transactional
    @Scheduled(cron = "0 0 */12 * * *")
    public void deleteChannelsLogs() {
        log.info("Start deleting expired channels logs");
        channelsLogsRepository.deleteExpiredLogs();
        log.info("Finished deleting expired channels logs");
    }
}
