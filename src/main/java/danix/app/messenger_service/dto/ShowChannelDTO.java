package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowChannelDTO {
    private int id;
    private String name;
    private String description;
    @JsonProperty("users_count")
    private int usersCount;
    @JsonProperty("created_at")
    private Date createdAt;
    private ResponseUserDTO owner;
    @JsonProperty("web_socket")
    private String webSocketUUID;
}
