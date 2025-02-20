package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseChannelDeletionDTO {

    @JsonProperty("deleted_channel_id")
    private Integer deletedChannelId;
}
