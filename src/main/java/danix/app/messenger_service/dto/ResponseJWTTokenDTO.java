package danix.app.messenger_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseJWTTokenDTO(@JsonProperty("jwt-token") String token) {
}
