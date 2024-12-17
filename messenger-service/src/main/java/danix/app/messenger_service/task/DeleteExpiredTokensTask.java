package danix.app.messenger_service.task;

import danix.app.messenger_service.repositories.TokensRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteExpiredTokensTask {
    private final TokensRepository tokensRepository;

    @Transactional
    @Scheduled(cron = "@midnight")
    public void run() {
        log.info("Start deleting expired jwt tokens");
        tokensRepository.deleteExpiredTokens();
        log.info("Finished deleting expired jwt tokens");
    }
}
