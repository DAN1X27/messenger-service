package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.ResponseImageDTO;
import org.springframework.web.multipart.MultipartFile;

public interface Image {
    void addImage(MultipartFile image, int id);

    void deleteImage(int id);

    ResponseImageDTO getImage(int id);
}
