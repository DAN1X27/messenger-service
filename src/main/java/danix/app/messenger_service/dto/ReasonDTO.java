package danix.app.messenger_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ReasonDTO(@NotBlank(message = "Reason must not be empty") String reason) {
}
