package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChannelUserDTO {
    private int id;
    private String username;
    @JsonProperty("admin")
    private Boolean isAdmin;
}
