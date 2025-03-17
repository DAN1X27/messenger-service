package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.messenger_service.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserInfoDTO {
    private Integer id;
    private String username;
    private String email;
    private String description;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("private")
    private boolean isPrivate;
    @JsonProperty("web_socket")
    private String webSocketUUID;
}
