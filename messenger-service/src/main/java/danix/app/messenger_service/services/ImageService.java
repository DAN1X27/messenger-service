package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.ResponseImageDTO;
import danix.app.messenger_service.util.ImageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class ImageService {

    public static void upload(Path imagesPath, MultipartFile multipartFile, String uuid) {
        if (!Objects.requireNonNull(multipartFile.getOriginalFilename()).endsWith(".png") &&
                !Objects.requireNonNull(multipartFile.getOriginalFilename()).endsWith(".jpg")) {
            throw new ImageException("Unsupported file");
        }
        File file = new File(imagesPath.toString(), uuid + (Objects.requireNonNull(multipartFile.getOriginalFilename().endsWith(".jpg") ? ".jpg" : ".png")));
        try (FileOutputStream outputStream = new FileOutputStream(file.toString())) {
            outputStream.write(multipartFile.getBytes());
        } catch (IOException e) {
            log.error("Error writing image, - {}", e.getMessage());
            throw new ImageException("Error writing image");
        }
    }

    public static ResponseImageDTO download(Path imagesPath, String objectUUID) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(imagesPath)) {
            for (Path file : directoryStream) {
                String fileName = file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf("."));
                if (fileName.equals(objectUUID)) {
                    byte[] imageData = Files.readAllBytes(file);
                    return new ResponseImageDTO(imageData, file.getFileName().endsWith(".jpg") ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG);
                }
            }
        } catch (IOException e) {
            log.error("Error download image, - {}", e.getMessage());
            throw new ImageException("Error reading image");
        }
        throw new ImageException("Image not found");
    }

    public static void delete(Path imagesPath, String objectUUID) {
        try {
            Files.deleteIfExists(Path.of(imagesPath.toString(), objectUUID + ".jpg"));
            Files.deleteIfExists(Path.of(imagesPath.toString(), objectUUID + ".png"));
        } catch (IOException e) {
            log.error("Error deleting image, - {}", e.getMessage());
            throw new ImageException("Error deleting image");
        }
    }
}
