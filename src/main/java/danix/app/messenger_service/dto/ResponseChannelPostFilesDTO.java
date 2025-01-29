package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseChannelPostFilesDTO {
    private long id;

    public ResponseChannelPostFilesDTO(long id) {
        this.id = id;
    }
}
