package danix.app.messenger_service.task;

import danix.app.messenger_service.repositories.GroupsInvitesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteExpiredGroupsInvitesTask {
    private final GroupsInvitesRepository groupsInvitesRepository;

    @Transactional
    @Scheduled(cron = "0 0 */12 * * *")
    public void run() {
        log.info("Start deleting expired groups invites task");
        groupsInvitesRepository.deleteExpiredInvites();
        log.info("Finished deleting expired groups invites task");
    }
}
