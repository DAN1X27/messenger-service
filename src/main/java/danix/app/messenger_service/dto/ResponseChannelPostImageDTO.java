package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChannelPostImageDTO {
    private long id;

    public ResponseChannelPostImageDTO(long id) {
        this.id = id;
    }
}
