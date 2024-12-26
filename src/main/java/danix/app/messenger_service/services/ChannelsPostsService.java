package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.ChannelException;
import danix.app.messenger_service.util.ImageException;
import danix.app.messenger_service.util.ImageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static danix.app.messenger_service.services.UserService.getCurrentUser;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ChannelsPostsService {
    private final ModelMapper modelMapper;
    private final ChannelsLogsRepository channelsLogsRepository;
    private final ChannelsPostsLikesRepository channelsPostsLikesRepository;
    private final ChannelsPostsRepository channelsPostsRepository;
    private final ChannelsPostsCommentsRepository commentsRepository;
    private final ChannelsService channelsService;
    private final ChannelsPostsImagesRepository imagesRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @Value("${channels_posts_images}")
    private String POSTS_IMAGES_PATH;
    @Value("${channels_posts_comments_images}")
    private String COMMENTS_IMAGES_PATH;


    @Transactional
    public void createPost(CreateChannelPostDTO post) {
        savePost(post.getText(), post.getChannelId(), ContentType.TEXT);
    }

    @Transactional
    public void createPost(MultipartFile image, int id) {
        String uuid = UUID.randomUUID().toString();
        ImageUtils.upload(Path.of(POSTS_IMAGES_PATH), image, uuid);
        ChannelPost post = savePost(null, id, ContentType.IMAGE);
        ChannelPostImage postImage = new ChannelPostImage();
        postImage.setPost(post);
        postImage.setImageUUID(uuid);
        imagesRepository.save(postImage);
    }

    private ChannelPost savePost(String text, int groupId, ContentType contentType) {
        Channel channel = channelsService.getById(groupId);
        ChannelUser user = channelsService.getChannelUser(getCurrentUser(), channel);
        if (user.getIsAdmin()) {
            ChannelPost post = new ChannelPost();
            post.setText(text);
            post.setOwner(user);
            post.setContentType(contentType);
            post.setChannel(channel);
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(getCurrentUser().getUsername() + " created post");
            channelLog.setChannel(channel);
            channelsPostsRepository.save(post);
            channelsLogsRepository.save(channelLog);
            ResponseChannelPostDTO postDTO = channelsService.convertToResponseChannelPostsDTO(post);
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getId(), postDTO);
            return post;
        }
        throw new ChannelException("Current user is not admin in this channel");
    }

    @Transactional
    public void addImage(long postId, MultipartFile image) {
        ChannelPost post = getById(postId);
        ChannelUser channelUser = channelsService.getChannelUser(getCurrentUser(), post.getChannel());
        if (channelUser.getIsAdmin()) {
            if (post.getImages().size() > 10) {
                throw new ImageException("Images limit exceeded");
            }
            String uuid = UUID.randomUUID().toString();
            ImageUtils.upload(Path.of(POSTS_IMAGES_PATH), image, uuid);
            ChannelPostImage postImage = new ChannelPostImage();
            postImage.setImageUUID(uuid);
            postImage.setPost(post);
            imagesRepository.save(postImage);
            if (post.getContentType() == ContentType.TEXT) {
                post.setContentType(ContentType.TEXT_IMAGE);
                post.getImages().add(postImage);
                messagingTemplate.convertAndSend("/topic/channel/" + channelUser.getId(),
                        new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostsDTO(post)));
            }
        } else {
            throw new ChannelException("Current user is not admin of this channel");
        }
    }

    public ResponseImageDTO getPostImage(long imageId) {
        ChannelPostImage image = imagesRepository.findById(imageId)
                .orElseThrow(() -> new ImageException("Image not found"));
        channelsService.getChannelUser(getCurrentUser(), image.getPost().getChannel());
        return ImageUtils.download(Path.of(POSTS_IMAGES_PATH), image.getImageUUID());
    }

    @Transactional
    public void deletePost(long postId) {
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        ChannelUser channelUser = channelsService.getChannelUser(getCurrentUser(), channel);
        if (channelUser.getIsAdmin()) {
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(getCurrentUser().getUsername() + " deleted post");
            channelLog.setChannel(channel);
            if (post.getImages() != null && !post.getImages().isEmpty()) {
                post.getImages().forEach(image -> ImageUtils.delete(Path.of(POSTS_IMAGES_PATH), image.getImageUUID()));
            }
            List<ChannelPostComment> comments = commentsRepository.findAllByPostIdAndContentType(postId, ContentType.IMAGE);
            if (comments != null && !comments.isEmpty()) {
                comments.forEach(comment -> ImageUtils.delete(Path.of(COMMENTS_IMAGES_PATH), comment.getText()));
            }
            channelsLogsRepository.save(channelLog);
            channelsPostsRepository.delete(post);
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getId(),
                    new ResponsePostDeletionDTO(postId));
        } else {
            throw new ChannelException("Current user is not admin of this channel");
        }
    }

    @Transactional
    public void addPostLike(long postId) {
        User currentUser = getCurrentUser();
        ChannelPost channelPost = getById(postId);
        ChannelUser channelUser = channelsService.getChannelUser(currentUser, channelPost.getChannel());
        channelsPostsLikesRepository.findByUserAndPost(currentUser, channelPost).ifPresent(like -> {
            throw new ChannelException("Current user already liked this post");
        });
        ChannelPostLike channelPostLike = new ChannelPostLike();
        channelPostLike.setPost(channelPost);
        channelPostLike.setUser(currentUser);
        ChannelPostLikeKey key = new ChannelPostLikeKey();
        key.setPostId(postId);
        key.setUserId(channelUser.getId());
        channelPostLike.setId(key);
        channelsPostsLikesRepository.save(channelPostLike);
        channelPost.getLikes().add(channelPostLike);
        messagingTemplate.convertAndSend("/topic/channel/" + channelPost.getChannel().getId(),
                new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostsDTO(channelPost)));
    }

    @Transactional
    public void deletePostLike(long postId) {
        User currentUser = getCurrentUser();
        ChannelPost post = getById(postId);
        channelsService.getChannelUser(currentUser, post.getChannel());
        channelsPostsLikesRepository.findByUserAndPost(currentUser, post).ifPresentOrElse(like -> {
            channelsPostsLikesRepository.delete(like);
            post.getLikes().remove(like);
            messagingTemplate.convertAndSend("/topic/channel/" + post.getChannel().getId(),
                    new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostsDTO(post)));
        }, () -> {
            throw new ChannelException("Post is not liked");
        });
    }

    @Transactional
    public void updatePost(UpdateChannelPostDTO post) {
        ChannelPost channelPost = getById(post.getId());
        Channel channel = channelPost.getChannel();
        User currentUser = getCurrentUser();
        ChannelUser channelUser = channelsService.getChannelUser(currentUser, channel);
        if (channelUser.getIsAdmin()) {
            if (channelPost.getContentType() == ContentType.IMAGE) {
                channelPost.setContentType(ContentType.TEXT_IMAGE);
            }
            channelPost.setText(post.getText());
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getId(),
                    new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostsDTO(channelPost)));
        } else {
            throw new ChannelException("Current user must be admin of this channel");
        }
    }

    @Transactional
    public void createComment(CreateChannelPostCommentDTO commentDTO) {
        saveComment(commentDTO.getPostId(), commentDTO.getComment(), ContentType.TEXT);
    }

    @Transactional
    public void createComment(long postId, MultipartFile image) {
        String uuid = UUID.randomUUID().toString();
        ImageUtils.upload(Path.of(COMMENTS_IMAGES_PATH), image, uuid);
        saveComment(postId, uuid, ContentType.IMAGE);
    }

    @Transactional
    public void saveComment(Long postId, String text, ContentType contentType) {
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        ChannelUser user = channelsService.getChannelUser(getCurrentUser(), channel);
        ChannelPostComment comment = new ChannelPostComment();
        comment.setText(text);
        comment.setOwner(user);
        comment.setPost(post);
        comment.setContentType(contentType);
        commentsRepository.save(comment);
        post.getComments().add(comment);
        messagingTemplate.convertAndSend("/topic/channel/" + channel.getId(),
                new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostsDTO(post)));
        messagingTemplate.convertAndSend("/topic/channel/post/" + post.getId() + "/comments",
                modelMapper.map(comment, ResponseChannelPostCommentDTO.class));
    }

    public ResponseImageDTO getCommentImage(long commentId) {
        ChannelPostComment comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new ChannelException("Comment not found"));
        Channel channel = comment.getPost().getChannel();
        channelsService.getChannelUser(getCurrentUser(), channel);
        if (comment.getContentType() != ContentType.IMAGE) {
            throw new ChannelException("Comment content type is not image");
        }
        return ImageUtils.download(Path.of(COMMENTS_IMAGES_PATH), comment.getText());
    }

    @Transactional
    public void deleteComment(long commentId) {
        ChannelPostComment comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new ChannelException("Comment not found"));
        ChannelPost post = comment.getPost();
        Channel channel = post.getChannel();
        User currentUser = getCurrentUser();
        ChannelUser channelUser = channelsService.getChannelUser(currentUser, channel);
        if (comment.getOwner().getUsername().equals(channelUser.getUsername()) || channelUser.getIsAdmin()) {
            if (comment.getContentType() == ContentType.IMAGE) {
                ImageUtils.delete(Path.of(COMMENTS_IMAGES_PATH), comment.getText());
            }
            commentsRepository.delete(comment);
            post.getComments().remove(comment);
            messagingTemplate.convertAndSend("/topic/channel/" + channel.getId(),
                    new ResponsePostUpdatingDTO(channelsService.convertToResponseChannelPostsDTO(post)));
            messagingTemplate.convertAndSend("/topic/channel/post/" + post.getId(),
                    new ResponsePostCommentDeletionDTO(commentId));
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
        channelsService.getChannelUser(currentUser, channel);
        if (comment.getOwner().getUsername().equals(currentUser.getUsername())) {
            if (comment.getContentType() == ContentType.TEXT) {
                comment.setText(commentDTO.getText());
                messagingTemplate.convertAndSend("/topic/channel/post/" + comment.getPost().getId(),
                        new ResponseCommentUpdatingDTO(comment.getId(), commentDTO.getText()));
            } else {
                throw new ChannelException("Comment cannot be updated");
            }
        } else {
            throw new ChannelException("Current user must be owner of comment");
        }
    }

    public List<ResponseChannelPostCommentDTO> getPostComments(long postId) {
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        channelsService.getChannelUser(getCurrentUser(), channel);
        return post.getComments().stream()
                .map(comment -> {
                    ResponseChannelPostCommentDTO commentDTO = modelMapper.map(comment, ResponseChannelPostCommentDTO.class);
                    User commentOwner = userService.getByUsername(comment.getOwner().getUsername());
                    commentDTO.setOwner(modelMapper.map(commentOwner, ResponseUserDTO.class));
                    return commentDTO;
                })
                .collect(Collectors.toList());
    }

    private ChannelPost getById(long id) {
        return channelsPostsRepository.findById(id)
                .orElseThrow(() -> new ChannelException("Post not found"));
    }
}
