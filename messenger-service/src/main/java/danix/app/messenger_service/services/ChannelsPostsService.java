package danix.app.messenger_service.services;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.util.ChannelException;
import danix.app.messenger_service.util.ImageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
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

    @Value("${channels_posts_images}")
    private String POSTS_IMAGES_PATH;
    @Value("${channels_posts_comments_images}")
    private String COMMENTS_IMAGES_PATH;

    @Transactional
    public void createPost(CreateChannelPostDTO post) {
        Channel channel = channelsService.getById(post.getChannelId());
        ChannelUser channelUser = channelsService.getChannelUser(getCurrentUser(), channel);
        if (channelUser.getIsAdmin()) {
            ChannelPost channelPost = new ChannelPost();
            channelPost.setPost(post.getText());
            channelPost.setChannel(channel);
            channelPost.setOwner(channelUser);
            channelPost.setContentType(ContentType.TEXT);
            channelsPostsRepository.save(channelPost);
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(getCurrentUser().getUsername() + " created post");
            channelLog.setChannel(channel);
            channelsLogsRepository.save(channelLog);
        } else {
            throw new ChannelException("Current user is not admin of this channel");
        }
    }

    @Transactional
    public void createPost(MultipartFile image, int id) {
        Channel channel = channelsService.getById(id);
        ChannelUser user = channelsService.getChannelUser(getCurrentUser(), channel);
        if (user.getIsAdmin()) {
            ChannelPost post = new ChannelPost();
            post.setOwner(user);
            post.setContentType(ContentType.IMAGE);
            post.setChannel(channel);
            ChannelPostImage postImage = new ChannelPostImage();
            String uuid = UUID.randomUUID().toString();
            postImage.setImageUUID(uuid);
            postImage.setPost(post);
            ImageService.upload(Path.of(POSTS_IMAGES_PATH), image, uuid);
            channelsPostsRepository.save(post);
            imagesRepository.save(postImage);
            ChannelLog channelLog = new ChannelLog();
            channelLog.setMessage(getCurrentUser().getUsername() + " created post");
            channelLog.setChannel(channel);
            channelsLogsRepository.save(channelLog);
        } else {
            throw new ChannelException("Current user is not admin of this channel");
        }
    }

    @Transactional
    public void addImage(long postId, MultipartFile image) {
        ChannelPost post = getById(postId);
        ChannelUser channelUser = channelsService.getChannelUser(getCurrentUser(), post.getChannel());
        if (channelUser.getIsAdmin()) {
            if (post.getImages().size() >= 10) {
                throw new ImageException("Images limit exceeded");
            }
            String uuid = UUID.randomUUID().toString();
            ImageService.upload(Path.of(POSTS_IMAGES_PATH), image, uuid);
            ChannelPostImage postImage = new ChannelPostImage();
            postImage.setImageUUID(uuid);
            postImage.setPost(post);
            imagesRepository.save(postImage);
            if (post.getContentType() == ContentType.TEXT) {
                post.setContentType(ContentType.TEXT_IMAGE);
            }
        } else {
            throw new ChannelException("Current user is not admin of this channel");
        }
    }
    
    public ResponseImageDTO getPostImage(long imageId) {
        ChannelPostImage image = imagesRepository.findById(imageId)
                        .orElseThrow(() -> new ImageException("Image not found"));
        channelsService.getChannelUser(getCurrentUser(), image.getPost().getChannel());
        return ImageService.download(Path.of(POSTS_IMAGES_PATH), image.getImageUUID());
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
                post.getImages().forEach(image -> ImageService.delete(Path.of(POSTS_IMAGES_PATH), image.getImageUUID()));
            }
            List<ChannelPostComment> comments = commentsRepository.findAllByPostIdAndContentType(postId, ContentType.IMAGE);
            if (comments != null && !comments.isEmpty()) {
                comments.forEach(comment -> ImageService.delete(Path.of(COMMENTS_IMAGES_PATH), comment.getComment()));
            }
            channelsLogsRepository.save(channelLog);
            channelsPostsRepository.delete(post);
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
    }

    @Transactional
    public void deletePostLike(long postId) {
        User currentUser = getCurrentUser();
        ChannelPost post = getById(postId);
        channelsService.getChannelUser(currentUser, post.getChannel());
        channelsPostsLikesRepository.findByUserAndPost(currentUser, post)
                .ifPresentOrElse(channelsPostsLikesRepository::delete, () -> {
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
            channelPost.setPost(post.getText());
        } else {
            throw new ChannelException("Current user must be admin of this channel");
        }
    }

    @Transactional
    public void createComment(CreateChannelPostCommentDTO commentDTO) {
        ChannelPost post = getById(commentDTO.getPostId());
        Channel channel = post.getChannel();
        ChannelUser user = channelsService.getChannelUser(getCurrentUser(), channel);
        ChannelPostComment comment = new ChannelPostComment();
        comment.setComment(commentDTO.getComment());
        comment.setOwner(user);
        comment.setPost(post);
        comment.setContentType(ContentType.TEXT);
        commentsRepository.save(comment);
    }

    @Transactional
    public void createComment(long postId, MultipartFile image) {
        ChannelPost post = getById(postId);
        Channel channel = post.getChannel();
        ChannelUser user = channelsService.getChannelUser(getCurrentUser(), channel);
        String uuid = UUID.randomUUID().toString();
        ChannelPostComment comment = new ChannelPostComment();
        comment.setComment(uuid);
        comment.setOwner(user);
        comment.setPost(post);
        comment.setContentType(ContentType.IMAGE);
        ImageService.upload(Path.of(COMMENTS_IMAGES_PATH), image, uuid);
        commentsRepository.save(comment);
    }

    public ResponseImageDTO getCommentImage(long commentId) {
        ChannelPostComment comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new ChannelException("Comment not found"));
        Channel channel = comment.getPost().getChannel();
        channelsService.getChannelUser(getCurrentUser(), channel);
        if (comment.getContentType() != ContentType.IMAGE) {
            throw new ChannelException("Comment content type is not image");
        }
        return ImageService.download(Path.of(COMMENTS_IMAGES_PATH), comment.getComment());
    }

    @Transactional
    public void deleteComment(long commentId) {
        ChannelPostComment comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new ChannelException("Comment not found"));
        Channel channel = comment.getPost().getChannel();
        User currentUser = getCurrentUser();
        ChannelUser channelUser = channelsService.getChannelUser(currentUser, channel);
        if (comment.getOwner().getUsername().equals(channelUser.getUsername()) || channelUser.getIsAdmin()) {
            if (comment.getContentType() == ContentType.IMAGE) {
                ImageService.delete(Path.of(COMMENTS_IMAGES_PATH), comment.getComment());
            }
            commentsRepository.delete(comment);
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
                comment.setComment(commentDTO.getComment());
            } else {
                throw new ChannelException("Current comment cannot be updated");
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
                .map(comment -> modelMapper.map(comment, ResponseChannelPostCommentDTO.class))
                .collect(Collectors.toList());
    }

    private ChannelPost getById(long id) {
        return channelsPostsRepository.findById(id)
                .orElseThrow(() -> new ChannelException("Post not found"));
    }
}
