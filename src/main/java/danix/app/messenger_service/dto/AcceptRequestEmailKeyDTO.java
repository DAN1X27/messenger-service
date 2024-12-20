package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcceptRequestEmailKeyDTO implements RequestEmailKey {
    @NotEmpty(message = "Email cannot be empty")
    private String email;
    @NotNull(message = "Key cannot be empty")
    private Integer key;
}
