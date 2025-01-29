package danix.app.messenger_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResponseGroupDTO {
    private int id;
    private String name;
    private String description;

    public ResponseGroupDTO(String name) {
        this.name = name;
    }
}
