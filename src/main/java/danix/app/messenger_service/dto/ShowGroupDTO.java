package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowGroupDTO {
    private int id;
    private String name;
    private ResponseUserDTO owner;
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonProperty("users_count")
    private int usersCount;
    @JsonProperty("description")
    private String description;
    @JsonProperty("web_socket")
    private String webSocketUUID;
}
