package danix.app.messenger_service.util;

import danix.app.messenger_service.dto.ResponseFileDTO;
import danix.app.messenger_service.models.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public final class FilesUtils {

    private FilesUtils() {
    }

    public static void upload(Path filesPath, MultipartFile multipartFile, String uuid, ContentType contentType) {
        File file;
        String name = Objects.requireNonNull(multipartFile.getOriginalFilename());
        String extension = name.substring(name.lastIndexOf('.'));
        String nameToSave = uuid + extension;
        switch (contentType) {
            case IMAGE -> {
                if (!extension.equals(".png") && !extension.equals(".jpg")) {
                    throw new FileException("Unsupported file");
                }
                file = new File(filesPath.toString(), nameToSave);
            }
            case VIDEO -> {
                if (!extension.equals(".mp4")) {
                    throw new FileException("Unsupported file");
                }
                file = new File(filesPath.toString(), nameToSave);
            }
            case AUDIO_OGG -> {
                if (!extension.equals(".ogg")) {
                    throw new FileException("Unsupported file");
                }
                file = new File(filesPath.toString(), nameToSave);
            }
            case AUDIO_MP3 -> {
                if (!extension.equals(".mp3")) {
                    throw new FileException("Unsupported file");
                }
                file = new File(filesPath.toString(), nameToSave);
            }
            default -> throw new FileException("Unsupported content type");
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(file.toString())) {
            outputStream.write(multipartFile.getBytes());
        } catch (IOException e) {
            log.error("Error uploading file - {}", e.getMessage());
            throw new FileException("Error while uploading file");
        }
    }

    public static ResponseFileDTO download(Path filesPath, String objectUUID, ContentType contentType) {
        Path file;
        MediaType mediaType;
        switch (contentType) {
            case IMAGE -> {
                file = Path.of(filesPath.toString(), objectUUID + ".jpg");
                if (!Files.exists(file)) {
                    file = Path.of(filesPath.toString(), objectUUID + ".png");
                    if (!Files.exists(file)) {
                        throw new FileException("File not found");
                    }
                }
                mediaType = file.getFileName().endsWith(".jpg") ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;
            }
            case VIDEO -> {
                file = Path.of(filesPath.toString(), objectUUID + ".mp4");
                if (!Files.exists(file)) {
                    throw new FileException("File not found");
                }
                mediaType = MediaType.parseMediaType("video/mp4");
            }
            case AUDIO_OGG -> {
                file = Path.of(filesPath.toString(), objectUUID + ".ogg");
                if (!Files.exists(file)) {
                    throw new FileException("File not found");
                }
                mediaType = MediaType.parseMediaType("audio/ogg");
            }
            case AUDIO_MP3 -> {
                file = Path.of(filesPath.toString(), objectUUID + ".mp3");
                if (!Files.exists(file)) {
                    throw new FileException("File not found");
                }
                mediaType = MediaType.parseMediaType("audio/mpeg");
            }
            default -> throw new FileException("Unsupported content type");
        }
        try {
            byte[] fileData = Files.readAllBytes(file);
            return new ResponseFileDTO(fileData, mediaType);
        } catch (IOException e) {
            log.error("Error download file - {}", e.getMessage());
            throw new FileException("Error download file");
        }
    }

    public static void delete(Path filesPath, String objectUUID) {
        try {
            Files.deleteIfExists(Path.of(filesPath.toString(), objectUUID + ".jpg"));
            Files.deleteIfExists(Path.of(filesPath.toString(), objectUUID + ".png"));
            Files.deleteIfExists(Path.of(filesPath.toString(), objectUUID + ".mp4"));
            Files.deleteIfExists(Path.of(filesPath.toString(), objectUUID + ".ogg"));
            Files.deleteIfExists(Path.of(filesPath.toString(), objectUUID + ".mp3"));
        } catch (IOException e) {
            log.error("Error deleting file, - {}", e.getMessage());
            throw new FileException("Error deleting file");
        }
    }
}