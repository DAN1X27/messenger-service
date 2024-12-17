package danix.app.messenger_service.repositories;

import danix.app.messenger_service.models.Channel;
import danix.app.messenger_service.models.ChannelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelsLogsRepository extends JpaRepository<ChannelLog, Long> {
    List<ChannelLog> findByChannel(Channel channel);

    @Modifying
    @Query("delete from ChannelLog where expiredTime <= current_timestamp")
    void deleteExpiredLogs();
}
