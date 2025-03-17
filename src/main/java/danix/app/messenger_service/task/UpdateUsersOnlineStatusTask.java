package danix.app.messenger_service.task;

import danix.app.messenger_service.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateUsersOnlineStatusTask {
    private final UserService userService;

    @Scheduled(cron = "0 */2 * * * *")
    public void run() {
        userService.setOfflineStatusForOfflineUsers();
    }
}
