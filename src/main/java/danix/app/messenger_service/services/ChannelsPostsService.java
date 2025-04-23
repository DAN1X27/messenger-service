package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.ChannelException;
import danix.app.messenger_service.util.FileException;
import danix.app.messenger_service.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ChannelsPostsService {
    private final ModelMapper modelMapper;
    private final ChannelsLogsRepository channelsLogsRepository;
    private final ChannelsPostsRepository channelsPostsRepository;
    private final ChannelsPostsCommentsRepository commentsRepository;
    private final ChannelsService channelsService;
    private final ChannelsPostsFilesRepository filesRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${channels_posts_images_path}")
    private String POSTS_IMAGES_PATH;
    @Value("${channels_posts_comments_images_path}")
    private String COMMENTS_IMAGES_PATH;
    @Value("${channels_posts_videos_path}")
    private String POSTS_VIDEOS_PATH;
    @Value("${channels_posts_comments_videos_path}")
    private String COMMENTS_VIDEOS_PATH;
    @Value("${channels_posts_audio_path}")
    private String POSTS_AUDIO_PATH;
    @Value("${channels_posts_comments_audio_path}")
    private String COMMENTS_AUDIO_PATH;

    @Transactional
    public long createPost(CreateChannelPostDTO post) {
        return savePost(post.getText(), post.getChannelId(), ContentType.TEXT).getId();
    }

    @Transactional
    public long createPost(MultipartFile file, int id, ContentType contentType) {
        String uuid = UUID.randomUUID().toString();
        switch (contentType) {
            case IMAGE -> FileUtils.upload(Path.of(POSTS_IMAGES_PATH), file, uuid, contentType);
            case VIDEO -> FileUtils.upload(Path.of(POSTS_VIDEOS_PATH), file, uuid, contentType);
            case AUDIO_MP3, AUDIO_OGG -> FileUtils.upload(Path.of(POSTS_AUDIO_PATH), file, uuid, contentType);
        }
        ChannelPost post = savePost(null, id, ContentType.IMAGE);
        ChannelPostFile postFile = new ChannelPostFile();
        postFile.setPost(post);
        postFile.setFileUUID(uuid);
        postFile.setContentType(contentType);
        filesRepository.save(postFile);
        return post.getId();
    }

    private ChannelPost savePost(String text, int groupId, ContentType contentType) {
        User curentUser = getCurrentUser();
        Channel channel = channelsService.getById(groupId);
        ChannelUser user = channelsService.getChannelUser(curentUser, channel);
        if (user.getIsAdmin()) {
            ChannelPost post = ChannelPost.builder()
                    .text(text)
                    .channel(channel)
                    .contentType(contentType)
                    .createdAt(LocalDateTime.now())
                    .build();
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(curentUser.getUsername() + " created post");
            channelLog.setChannel(channel);
            channelsPostsRepository.save(post);
            channelsLogsRepository.save(channelLog);
            ResponseChannelPostDTO postDTO = channelsService.convertToResponseChannelPostDTO(post);
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(), postDTO);
            return post;
        }
        throw new ChannelException("Current user is not admin in this channel");
    }

    @Transactional
    public long addFile(long postId, MultipartFile file, ContentType contentType) {
        User curentUser = getCurrentUser();
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        ChannelUser channelUser = channelsService.getChannelUser(curentUser, channel);
        if (channelUser.getIsAdmin() && post.getOwner().getUser().getId() == curentUser.getId()) {
            if (post.getFiles().size() >= 10) {
                throw new ChannelException("Files limit exceeded");
            }
            String uuid = UUID.randomUUID().toString();
            switch (contentType) {
                case IMAGE -> FileUtils.upload(Path.of(POSTS_IMAGES_PATH), file, uuid, contentType);
                case VIDEO -> FileUtils.upload(Path.of(POSTS_VIDEOS_PATH), file, uuid, contentType);
                case AUDIO_MP3, AUDIO_OGG -> FileUtils.upload(Path.of(POSTS_AUDIO_PATH), file, uuid, contentType);
                default -> throw new ChannelException("Unsupported content type");
            }
            ChannelPostFile postFile = new ChannelPostFile();
            postFile.setFileUUID(uuid);
            postFile.setPost(post);
            postFile.setContentType(contentType);
            filesRepository.save(postFile);
            if (post.getContentType() == ContentType.TEXT) {
                post.setContentType(ContentType.TEXT_FILE);
            }
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                    new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostDTO(post)));
            return postFile.getId();
        } else {
            throw new ChannelException("Current user must be admin of channel or owner of post");
        }
    }

    public ResponseFileDTO getPostFile(long fileId) {
        ChannelPostFile file = filesRepository.findById(fileId)
                .orElseThrow(() -> new FileException("File not found"));
        channelsService.getChannelUser(getCurrentUser(), file.getPost().getChannel());
        switch (file.getContentType()) {
            case IMAGE -> {
                return FileUtils.download(Path.of(POSTS_IMAGES_PATH), file.getFileUUID(), file.getContentType());
            }
            case VIDEO -> {
                return FileUtils.download(Path.of(POSTS_VIDEOS_PATH), file.getFileUUID(), file.getContentType());
            }
            case AUDIO_MP3, AUDIO_OGG -> {
                return FileUtils.download(Path.of(POSTS_AUDIO_PATH), file.getFileUUID(), file.getContentType());
            }
            default -> throw new FileException("File not found");
        }
    }

    @Transactional
    public void deletePost(long postId) {
        User curentUser = getCurrentUser();
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        ChannelUser channelUser = channelsService.getChannelUser(curentUser, channel);
        if (channelUser.getIsAdmin()) {
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(channelUser.getUsername() + " deleted post");
            channelLog.setChannel(channel);
            post.getFiles().forEach(file -> {
                switch (file.getContentType()) {
                    case IMAGE -> FileUtils.delete(Path.of(POSTS_IMAGES_PATH), file.getFileUUID());
                    case VIDEO -> FileUtils.delete(Path.of(POSTS_VIDEOS_PATH), file.getFileUUID());
                    case AUDIO_MP3, AUDIO_OGG -> FileUtils.delete(Path.of(POSTS_AUDIO_PATH), file.getFileUUID());
                }
            });
            post.getComments().forEach(comment -> {
                switch (comment.getContentType()) {
                    case IMAGE -> FileUtils.delete(Path.of(COMMENTS_IMAGES_PATH), comment.getText());
                    case VIDEO -> FileUtils.delete(Path.of(COMMENTS_VIDEOS_PATH), comment.getText());
                    case AUDIO_OGG, AUDIO_MP3 -> FileUtils.delete(Path.of(COMMENTS_AUDIO_PATH), comment.getText());
                }
            });
            channelsLogsRepository.save(channelLog);
            jdbcTemplate.update("DELETE FROM channels_posts where id = ?", postId);
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                    Map.of("deleted_post_id", postId));
        } else {
            throw new ChannelException("Current user is not admin of this channel");
        }
    }

    @Transactional
    public void addPostLike(long postId) {
        User currentUser = userService.getById(getCurrentUser().getId());
        ChannelPost post = getById(postId);
        channelsService.getChannelUser(currentUser, post.getChannel());
        if (post.getLikes().contains(currentUser)) {
            throw new ChannelException("Post already liked");
        }
        post.getLikes().add(currentUser);
    }

    @Transactional
    public void deletePostLike(long postId) {
        User currentUser = userService.getById(getCurrentUser().getId());
        ChannelPost post = getById(postId);
        channelsService.getChannelUser(currentUser, post.getChannel());
        if (!post.getLikes().contains(currentUser)) {
            throw new ChannelException("Post is not liked");
        }
        post.getLikes().remove(currentUser);
    }

    @Transactional
    public void updatePost(UpdateChannelPostDTO post) {
        ChannelPost channelPost = getById(post.getId());
        Channel channel = channelPost.getChannel();
        User currentUser = getCurrentUser();
        ChannelUser channelUser = channelsService.getChannelUser(currentUser, channel);
        if (channelUser.getIsAdmin() && channelPost.getOwner().getUser().getId() == currentUser.getId()) {
            if (channelPost.getContentType() == ContentType.IMAGE) {
                channelPost.setContentType(ContentType.TEXT_FILE);
            }
            channelPost.setText(post.getText());
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID(),
                    new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostDTO(channelPost)));
        } else {
            throw new ChannelException("Current user must be admin of this channel and owner of post");
        }
    }

    @Transactional
    public long createComment(CreateChannelPostCommentDTO commentDTO) {
        return saveComment(commentDTO.getPostId(), commentDTO.getComment(), ContentType.TEXT);
    }

    @Transactional
    public long createComment(long postId, MultipartFile file, ContentType contentType) {
        String uuid = UUID.randomUUID().toString();
        Path path;
        switch (contentType) {
            case IMAGE -> {
                path = Path.of(COMMENTS_IMAGES_PATH);
                FileUtils.upload(path, file, uuid, contentType);
            }
            case VIDEO -> {
                path = Path.of(COMMENTS_VIDEOS_PATH);
                FileUtils.upload(path, file, uuid, contentType);
            }
            case AUDIO_MP3, AUDIO_OGG -> {
                path = Path.of(COMMENTS_AUDIO_PATH);
                FileUtils.upload(path, file, uuid, contentType);
            }
            default -> throw new ChannelException("Unsupported content type");
        }
        try {
            return saveComment(postId, uuid, contentType);
        } catch (ChannelException e) {
            FileUtils.delete(path, uuid);
            throw e;
        }
    }

    private long saveComment(Long postId, String text, ContentType contentType) {
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        ChannelUser user = channelsService.getChannelUser(getCurrentUser(), channel);
        if (!channel.isPostsCommentsAllowed()) {
            throw new ChannelException("Comments are not allowed in this channel");
        }
        boolean isFile = false;
        if (contentType != ContentType.TEXT) {
            if (!channel.isFilesAllowed()) {
                throw new ChannelException("Files are not allowed in this channel");
            }
            isFile = true;
        }
        ChannelPostComment comment = ChannelPostComment.builder()
                .text(text)
                .owner(user)
                .contentType(contentType)
                .post(post)
                .createdAt(LocalDateTime.now())
                .build();
        commentsRepository.save(comment);
        ResponseChannelPostCommentDTO commentDTO = modelMapper.map(comment, ResponseChannelPostCommentDTO.class);
        if (isFile) {
            commentDTO.setText(null);
        }
        commentDTO.setOwner(modelMapper.map(comment.getOwner(), ResponseUserDTO.class));
        messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID() + "/post/" + post.getId() + "/comments", commentDTO);
        return comment.getId();
    }

    public ResponseFileDTO getCommentFile(long commentId) {
        ChannelPostComment comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new ChannelException("Comment not found"));
        Channel channel = comment.getPost().getChannel();
        channelsService.getChannelUser(getCurrentUser(), channel);
        switch (comment.getContentType()) {
            case IMAGE -> {
                return FileUtils.download(Path.of(COMMENTS_IMAGES_PATH), comment.getText(), comment.getContentType());
            }
            case VIDEO -> {
                return FileUtils.download(Path.of(COMMENTS_VIDEOS_PATH), comment.getText(), comment.getContentType());
            }
            case AUDIO_MP3, AUDIO_OGG -> {
                return FileUtils.download(Path.of(COMMENTS_AUDIO_PATH), comment.getText(), comment.getContentType());
            }
            default -> throw new ChannelException("Comment is not file");
        }
    }

    @Transactional
    public void deleteComment(long commentId) {
        User currentUser = getCurrentUser();
        ChannelPostComment comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new ChannelException("Comment not found"));
        ChannelPost post = comment.getPost();
        Channel channel = post.getChannel();
        ChannelUser channelUser = channelsService.getChannelUser(currentUser, channel);
        if (comment.getOwner().getId() == channelUser.getId() || channelUser.getIsAdmin()) {
            switch (comment.getContentType()) {
                case IMAGE -> FileUtils.delete(Path.of(COMMENTS_IMAGES_PATH), comment.getText());
                case VIDEO -> FileUtils.delete(Path.of(COMMENTS_VIDEOS_PATH), comment.getText());
                case AUDIO_MP3, AUDIO_OGG -> FileUtils.delete(Path.of(COMMENTS_AUDIO_PATH), comment.getText());
            }
            commentsRepository.delete(comment);
            post.getComments().remove(comment);
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID() + "/post/" + post.getId() + "/comments",
                    Map.of("deleted_comment_id", commentId));
        } else {
            throw new ChannelException("Current user must be owner of comment or admin");
        }
    }

    @Transactional
    public void updateComment(UpdateChannelPostCommentDTO commentDTO, long id) {
        ChannelPostComment comment = commentsRepository.findById(id)
                .orElseThrow(() -> new ChannelException("Comment not found"));
        Channel channel = comment.getPost().getChannel();
        User currentUser = getCurrentUser();
        ChannelUser channelUser = channelsService.getChannelUser(currentUser, channel);
        if (comment.getOwner().getId() == channelUser.getId()) {
            if (comment.getContentType() == ContentType.TEXT) {
                comment.setText(commentDTO.getText());
                messagingTemplate.convertAndSend("/topic/channel/" + channel.getWebSocketUUID() + "/post/" +
                                                 comment.getPost().getId() + "/comments", new ResponseCommentUpdatingDTO(comment.getId(), commentDTO.getText()));
            } else {
                throw new ChannelException("File cannot be updated");
            }
        } else {
            throw new ChannelException("Current user must be owner of comment");
        }
    }

    public List<ResponseChannelPostCommentDTO> getPostComments(long postId, int page, int count) {
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        channelsService.getChannelUser(getCurrentUser(), channel);
        if (!channel.isPostsCommentsAllowed()) {
            return Collections.emptyList();
        }
        return commentsRepository.findAllByPost(post, PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "id"))).stream()
                .map(comment -> {
                    ResponseChannelPostCommentDTO commentDTO = modelMapper.map(comment, ResponseChannelPostCommentDTO.class);
                    User commentOwner = userService.getByUsername(comment.getOwner().getUsername());
                    commentDTO.setOwner(modelMapper.map(commentOwner, ResponseUserDTO.class));
                    if (comment.getContentType() != ContentType.TEXT) {
                        commentDTO.setText(null);
                    }
                    return commentDTO;
                }).toList();
    }

    private ChannelPost getById(long id) {
        return channelsPostsRepository.findById(id)
                .orElseThrow(() -> new ChannelException("Post not found"));
    }
}
