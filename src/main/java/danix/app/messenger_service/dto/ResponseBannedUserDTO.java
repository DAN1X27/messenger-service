package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// TODO: DELETE
@Getter
@Setter
@AllArgsConstructor
@Deprecated
public class ResponseBannedUserDTO {
    private Integer bannedUserId;
}
