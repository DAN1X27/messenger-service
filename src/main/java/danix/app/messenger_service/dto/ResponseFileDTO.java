package danix.app.messenger_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;

@Getter
@Setter
@AllArgsConstructor
public class ResponseFileDTO {
    private byte[] fileData;
    private MediaType type;
}
