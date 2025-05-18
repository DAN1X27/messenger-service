package danix.app.messenger_service.api;

import danix.app.messenger_service.dto.IdDTO;
import danix.app.messenger_service.dto.MessageDTO;
import danix.app.messenger_service.dto.ResponseChatDTO;
import danix.app.messenger_service.dto.ShowChatDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatsAPI {

    @Operation(summary = "Returns all user's chats")
    ResponseEntity<List<ResponseChatDTO>> getAll();

    @Operation(
            summary = "Returns chat info",
            description = "Finds chat by id and returns its info and messages"
    )
    ResponseEntity<ShowChatDTO> show(@Parameter(description = "Chat id", example = "15") int id,
                                     @Parameter(description = "Page of messages", example = "0") int page,
                                     @Parameter(description = "Count of messages per page", example = "10") int count);

    @Operation(
            summary = "Deletes chat",
            description = "Finds chat by id and deletes it"
    )
    ResponseEntity<HttpStatus> delete(@Parameter(description = "Chat id", example = "15") int id);

    @Operation(
            summary = "Creates chat",
            description = "Find user by id and creates chat with him"
    )
    @ApiResponse(description = "Chat id")
    ResponseEntity<IdDTO> create(@Parameter(description = "User id", example = "10") int userId);

    @Operation(
            summary = "Sends text message",
            description = "Finds chat by id and sends text message to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendMessage(@Parameter(description = "Chat id", example = "15") int id, MessageDTO messageDTO,
                                      BindingResult bindingResult);

    @Operation(
            summary = "Sends image",
            description = "Finds chat by id and sends image to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendImage(@RequestBody(description = "Image", content = {
                                            @Content(mediaType = "image/jpeg"),
                                            @Content(mediaType = "image/png")
                                    }) MultipartFile image,
                                    @Parameter(description = "Chat id", example = "15") int id);

    @Operation(
            summary = "Sends video",
            description = "Finds chat by id and sends video to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendVideo(@RequestBody(description = "Video", content = {
                                            @Content(mediaType = "video/mp4")
                                    }) MultipartFile video,
                                    @Parameter(description = "Chat id", example = "15") int id);

    @Operation(
            summary = "Sends audio mp3",
            description = "Finds chat by id and sends audio mp3 to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendAudioMP3(@RequestBody(description = "audio", content = {
                                               @Content(mediaType = "audio/mp3")
                                       }) MultipartFile audio,
                                       @Parameter(description = "Chat id", example = "15") int id);

    @Operation(
            summary = "Sends audio ogg",
            description = "Finds chat by id and sends audio ogg to it"
    )
    @ApiResponse(description = "Message id")
    ResponseEntity<IdDTO> sendAudioOgg(@RequestBody(description = "audio", content = {
                                               @Content(mediaType = "audio/ogg")
                                        }) MultipartFile audio,
                                       @Parameter(description = "Chat id", example = "15") int id);

    @Operation(
            summary = "Returns messages file",
            description = "Finds message by id and returns its file"
    )
    ResponseEntity<?> getMessageFile(@Parameter(description = "Message id", example = "20") long id);

    @Operation(
            summary = "Deletes message",
            description = "Finds message by id and deletes it"
    )
    ResponseEntity<HttpStatus> deleteMessage(@Parameter(description = "Message id", example = "20") long id);

    @Operation(
            summary = "Updates message",
            description = "Finds message by id and updates it"
    )
    ResponseEntity<HttpStatus> updateMessage(@Parameter(description = "Message id", example = "15") long id,
                                             MessageDTO messageDTO, BindingResult bindingResult);

}
