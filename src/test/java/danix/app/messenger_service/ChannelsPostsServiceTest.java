package danix.app.messenger_service;

import danix.app.messenger_service.dto.*;
import danix.app.messenger_service.models.*;
import danix.app.messenger_service.repositories.*;
import danix.app.messenger_service.security.UserDetailsImpl;
import danix.app.messenger_service.services.ChannelsPostsService;
import danix.app.messenger_service.services.ChannelsService;
import danix.app.messenger_service.services.UserService;
import danix.app.messenger_service.util.ChannelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import util.TestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChannelsPostsServiceTest {

    private final User currentUser = TestUtils.getTestCurrentUser();

    private final User testUser = TestUtils.getTestUser();

    private final Channel testChannel = TestUtils.getTestChannel();

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChannelsPostsFilesRepository filesRepository;

    @Mock
    private ChannelsService channelsService;

    @Mock
    private ChannelsPostsCommentsRepository commentsRepository;

    @Mock
    private ChannelsPostsRepository postsRepository;

    @Mock
    private ChannelsLogsRepository logsRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ChannelsPostsService postsService;

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
    }

    @Test
    public void createPost() {
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        when(channelsService.getById(testChannel.getId())).thenReturn(testChannel);
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(channelUser);
        CreateChannelPostDTO createChannelPostDTO = new CreateChannelPostDTO();
        createChannelPostDTO.setChannelId(testChannel.getId());
        createChannelPostDTO.setText("test text");
        postsService.createPost(createChannelPostDTO);
        verify(postsRepository, times(1)).save(any(ChannelPost.class));
        verify(logsRepository, times(1)).save(any(ChannelLog.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" + testChannel.getWebSocketUUID()),
                any(ResponseChannelPostDTO.class));
    }

    @Test
    public void createPostWhenCurrentUserIsNotAdminOfChannel() {
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        when(channelsService.getById(testChannel.getId())).thenReturn(testChannel);
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(channelUser);
        CreateChannelPostDTO createChannelPostDTO = new CreateChannelPostDTO();
        createChannelPostDTO.setChannelId(testChannel.getId());
        assertThrows(ChannelException.class, () -> postsService.createPost(createChannelPostDTO));
    }

    @Test
    public void deletePost() {
        ChannelPost testPost = new ChannelPost();
        testPost.setId(1L);
        testPost.setChannel(testChannel);
        testPost.setFiles(Collections.emptyList());
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        channelUser.setUser(currentUser);
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(channelUser);
        when(commentsRepository.findAllByPostAndContentTypeIsNot(eq(testPost), eq(ContentType.TEXT), any()))
                .thenReturn(Collections.emptyList());
        CompletableFuture<Void> future = postsService.deletePost(1L);
        future.join();
        verify(logsRepository, times(1)).save(any(ChannelLog.class));
        verify(postsRepository).deleteById(testPost.getId());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" + testChannel.getWebSocketUUID()),
                any(Map.class));
    }

    @Test
    public void deletePostWhenPostNotFound() {
        when(postsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> postsService.deletePost(1L));
    }

    @Test
    public void deletePostWhenCurrentUserIsNotAdminOfChannel() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(channelUser);
        assertThrows(ChannelException.class, () -> postsService.deletePost(1L));
    }

    @Test
    public void updatePost() {
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        channelUser.setUser(currentUser);
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setText("test text");
        testPost.setOwner(channelUser);
        testPost.setContentType(ContentType.TEXT);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(channelUser);
        UpdateChannelPostDTO updateChannelPostDTO = new UpdateChannelPostDTO();
        updateChannelPostDTO.setId(1L);
        updateChannelPostDTO.setText("new text");
        postsService.updatePost(updateChannelPostDTO);
        assertEquals(testPost.getText(), updateChannelPostDTO.getText());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" + testChannel.getWebSocketUUID()),
                any(ResponsePostUpdatingDTO.class));
    }

    @Test
    public void updatePostWhenCurrentUserIsNotAdminOfTheChannel() {
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(false);
        channelUser.setUser(currentUser);
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setOwner(channelUser);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(channelUser);
        UpdateChannelPostDTO updateChannelPostDTO = new UpdateChannelPostDTO();
        updateChannelPostDTO.setId(1L);
        assertThrows(ChannelException.class, () -> postsService.updatePost(updateChannelPostDTO));
    }

    @Test
    public void updatePostWhenCurrentUserIsAdminButNotOwnerOfPost() {
        ChannelUser channelUser = new ChannelUser();
        channelUser.setIsAdmin(true);
        channelUser.setUser(testUser);
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setOwner(channelUser);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(channelUser);
        UpdateChannelPostDTO updateChannelPostDTO = new UpdateChannelPostDTO();
        updateChannelPostDTO.setId(1L);
        assertThrows(ChannelException.class, () -> postsService.updatePost(updateChannelPostDTO));
    }

    @Test
    public void addPostLike() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setLikes(new ArrayList<>());
        when(userService.getById(currentUser.getId())).thenReturn(currentUser);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(new ChannelUser());
        postsService.addPostLike(1L);
        assertFalse(testPost.getLikes().isEmpty());
    }

    @Test
    public void addPostLikeWhenPostAlreadyLiked() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setLikes(Collections.singletonList(currentUser));
        when(userService.getById(currentUser.getId())).thenReturn(currentUser);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(new ChannelUser());
        assertThrows(ChannelException.class, () -> postsService.addPostLike(1L));
    }

    @Test
    public void addPostLikeWhenPostNotFound() {
        when(postsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> postsService.addPostLike(1L));
    }

    @Test
    public void deletePostLike() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setLikes(new ArrayList<>());
        testPost.getLikes().add(currentUser);
        when(userService.getById(currentUser.getId())).thenReturn(currentUser);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        postsService.deletePostLike(1L);
        assertTrue(testPost.getLikes().isEmpty());
    }

    @Test
    public void deletePostLikeWhenPostNotLiked() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setLikes(Collections.emptyList());
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        assertThrows(ChannelException.class, () -> postsService.deletePostLike(1L));
    }

    @Test
    public void deletePostLikeWhenPostNotFound() {
        when(postsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> postsService.deletePostLike(1L));
    }

    @Test
    public void createPostComment() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        testPost.setComments(new ArrayList<>());
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(new ChannelUser());
        ResponseChannelPostCommentDTO responseCommentDTO = new ResponseChannelPostCommentDTO();
        responseCommentDTO.setText("test text");
        when(modelMapper.map(any(ChannelPostComment.class), eq(ResponseChannelPostCommentDTO.class)))
                .thenReturn(responseCommentDTO);
        CreateChannelPostCommentDTO commentDTO = new CreateChannelPostCommentDTO();
        commentDTO.setComment("test comment");
        commentDTO.setPostId(1L);
        postsService.createComment(commentDTO);
        verify(commentsRepository, times(1)).save(any(ChannelPostComment.class));
        assertNotNull(responseCommentDTO.getText());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/channel/" +
                        testChannel.getWebSocketUUID() + "/post/0/comments"),
                any(ResponseChannelPostCommentDTO.class));
    }

    @Test
    public void createPostCommentWhenCommentsNotAllowed() {
        testChannel.setPostsCommentsAllowed(false);
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        when(postsRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(new ChannelUser());
        CreateChannelPostCommentDTO commentDTO = new CreateChannelPostCommentDTO();
        commentDTO.setComment("test comment");
        commentDTO.setPostId(1L);
        assertThrows(ChannelException.class, () -> postsService.createComment(commentDTO));
    }

    @Test
    public void deletePostCommentWhenCurrentUserOwnerOfComment() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        ChannelUser testChannelUser = new ChannelUser();
        ChannelPostComment testComment = new ChannelPostComment();
        testComment.setPost(testPost);
        testComment.setOwner(testChannelUser);
        testComment.setContentType(ContentType.TEXT);
        testPost.setComments(new ArrayList<>());
        testPost.getComments().add(testComment);
        when(commentsRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(testChannelUser);
        postsService.deleteComment(1L);
        verify(commentsRepository, times(1)).delete(testComment);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/channel/" + testChannel.getWebSocketUUID() + "/post/0/comments"), any(Map.class));
        assertTrue(testPost.getComments().isEmpty());
    }

    @Test
    public void deletePostCommentWhenCurrentUserNotCommentOwnerAndAdminOfChannel() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        ChannelUser testChannelUser = new ChannelUser();
        testChannelUser.setIsAdmin(true);
        testChannelUser.setId(1);
        ChannelUser commentOwner = new ChannelUser();
        commentOwner.setId(2);
        ChannelPostComment testComment = new ChannelPostComment();
        testComment.setOwner(commentOwner);
        testComment.setContentType(ContentType.TEXT);
        testComment.setPost(testPost);
        testPost.setComments(new ArrayList<>());
        testPost.getComments().add(testComment);
        when(commentsRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(testChannelUser);
        postsService.deleteComment(1L);
        verify(commentsRepository, times(1)).delete(testComment);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/channel/" + testChannel.getWebSocketUUID() + "/post/0/comments"), any(Map.class));
        assertTrue(testPost.getComments().isEmpty());
    }

    @Test
    public void deletePostCommentWhenCommentNotFound() {
        when(commentsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChannelException.class, () -> postsService.deleteComment(1L));
    }

    @Test
    public void updatePostComment() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        ChannelUser testChannelUser = new ChannelUser();
        testChannelUser.setId(1);
        ChannelPostComment testComment = new ChannelPostComment();
        testComment.setPost(testPost);
        testComment.setContentType(ContentType.TEXT);
        testComment.setText("test comment");
        testComment.setOwner(testChannelUser);
        when(commentsRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(testChannelUser);
        UpdateChannelPostCommentDTO commentDTO = new UpdateChannelPostCommentDTO();
        commentDTO.setText("new comment");
        postsService.updateComment(commentDTO, 1L);
        assertEquals(commentDTO.getText(), testComment.getText());
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/channel/" + testChannel.getWebSocketUUID() + "/post/0/comments"), any(ResponseCommentUpdatingDTO.class));
    }

    @Test
    public void updatePostCommentWhenCommentContentTypeIsNotText() {
        ChannelPost testPost = new ChannelPost();
        testPost.setChannel(testChannel);
        ChannelUser testChannelUser = new ChannelUser();
        testChannelUser.setId(1);
        ChannelPostComment testComment = new ChannelPostComment();
        testComment.setPost(testPost);
        testComment.setContentType(ContentType.IMAGE);
        testComment.setOwner(testChannelUser);
        when(commentsRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(channelsService.getChannelUser(currentUser, testChannel)).thenReturn(testChannelUser);
        assertThrows(ChannelException.class, () -> postsService.updateComment(new UpdateChannelPostCommentDTO(), 1L));
    }
}