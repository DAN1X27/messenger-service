package danix.app.messenger_service.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "email_keys")
@Getter
@Setter
public class EmailKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String email;
    private Integer key;
    @Column(name = "expired_time")
    private LocalDateTime expiredTime;
    private int attempts;

    public EmailKey() {
        expiredTime = LocalDateTime.now().plusMinutes(2);
    }

    public EmailKey(String email, int key) {
        this.email = email;
        this.key = key;
        expiredTime = LocalDateTime.now().plusMinutes(2);
    }
}
