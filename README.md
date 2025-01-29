#DESCRIPTION
-
```
This is messenger on Java Spring Boot, to test this project use postman.
```

#LAUNCH
-
```
Downlod and launch 'Docker Desktop' and use commands: 'docker network create messenger-service-net', 
'docker-compose up --build' to launch project, after that launch project 'messenger-service-email-sender'.
Use command 'docker-compose down' to turn off project.
The database is available at 'jdbc:postgresql://localhost:5433/messenger_service_db'.
```
#ENDPOINTS
-
```
Use 'localhost:8080' for all endpoints.
```
**AUTHENTICATION**

* POST /auth/login
```
-ACTION: Login user.
-BODY: email, password
-RETURNS: Authentication jwt token.
```
* POST /auth/registration
```
-ACTION: Temporal registers the user and sends him a confirmation code by email.
-BODY: email, password, username, description(not required), isPrivate(not required).
-RETURNS: Http status 'CREATED'.
```
* PATCH /auth/registration/accept
```
-ACTION: BODY registration key and registers user.
-ACCETPS: email, key(the key is in your email).
-RETURNS: Authentication jwt token.
```
* POST /auth/password
```
-ACTION: Sends email for recover password if user forgot him.
-BODY: email.
-RETURNS: Http status 'CREATED'.
```
* PATCH /auth/password
```
-ACTION: Recover user password.
-BODY: email, newPassword, key.
-RETURNS: Http status 'OK'.
```
**USER**

* GET /user/info
```
-ACTION: Returns all info about current user.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* GET /user/image
```
-ACTION: Returns user image.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* PATCH /user/image
```
-ACTION: Update user image.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .png or .jpg file.
-RETURNS: Http status 'CREATED'.
```
* DELETE /user/image
```
-ACTION: Delete user image.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /user/status
```
-ACTION: Update user online status.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* GET /user/notifications
```
-ACTION: Returns all user notifications.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* GET /user/find
```
-ACTION: Returns user by name.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: username.
```
* GET /user/{id}/image
```
-ACTION: Reuturns user image by user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* PATCH /user/private
```
-ACTION: Update user private status.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* GET /user/friends/requests
```
-ACTION: Returns all user friends requests.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* POST /user/friend/{id}
```
-ACTION: Add friend by user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* DELETE /user/friend/{id}
```
-ACTION: Delete friend by user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* GET /user/friends
```
-ACTION: Returns all user friends.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* DELETE /user/friend/request/cancel/{id}
```
-ACTION: Cancel friend requests by user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* DELETE /user/friend/request/reject/{id}
```
-ACTION: Reject friend request by user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /user/friend/request/{id}
```
-ACTION: Accept friend reques by user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* POST /user/block/{id}
```
-ACTION: Block user by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* DELETE /user/unblock/{id}
```
-ACTION: Unblock user by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /user/username
```
-ACTION: Update username.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: username(new username), password.
-RETURNS: Http status 'OK'.
```
* PATCH /user/password
```
-ACTION: Update password.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: password(current password), newPassword.
-RETURNS: Http status 'OK'.
```
**ADMIN**
```
To use these endpoints the user must have a role 'ADMIN'.
You can change the user role in the database console with the command 'update table person set role = 'ROLE_ADMIN' where id = id'.
```
* PATCH /admin/user/ban/{id}
```
-ACTION: Ban user by id and send email message to him.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: reason.
-RETURNS: Http status 'OK'.
```
* PATCH /admin/user/unban/{id}
```
-ACTION: Unban user by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /admin/channel/ban/{id}
```
-ACTION: Ban channel by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: reason.
-RETURNS: Http status 'OK'.
```
* PATCH /admin/channel/unban/{id}
```
-ACTION: Unban channel by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
**CHATS**
* GET /chats
```
-ACTION: Returns all user chats.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* POST /chats/{id}
```
-ACTION: Create chat with user by user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-PARAMS: page - page number of messages (min value = 0), count - count messages for 1 page.
-RETURNS: Http status 'CREATED'.
```
* GET /chats/{id}
```
-ACTION: Returns user chat by chat id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* POST /chats/{id}/message
```
-ACTION: Send text message to chat by chat id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* POST /chats/{id}/message/image
```
-ACTION: Send image to chat by chat id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .jpg or .png file.
-RETURNS: Http status 'CREATED'.
```
* POST /chats/{id}/message/video
```
-ACTION: Send video to chat by chat id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp4 file.
-RETURNS: Http status 'CREATED'.
```
* POST /chats/{id}/message/audio/mp3
```
-ACTION: Send audio mp3 to chat by chat id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp3 file.
-RETURNS: Http status 'CREATED'.
```
* POST /chats/{id}/message/audio/ogg
```
-ACTION: Send audio ogg to chat by chat id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .ogg file.
-RETURNS: Http status 'CREATED'.
```
* GET /chats/message/{id}/file
```
-ACTION: Returns message file by message id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* DELETE /chats/message/{id}
```
-ACTION: Delete message by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /chats/message/{id}
```
-ACTION: Update message by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: message(updated message).
-RETURNS: Http status 'OK'.
```
**GROUPS**
* GET /groups
```
-ACTION: Returns user groups.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* GET /group/{id}
```
-ACTION: Returns user group by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* POST /groups
```
-ACTION: Create new group.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: name (group name), description (group description, not required).
RETURNS: Http status 'CREATED'.
```
* PATCH /groups
```
-ACTION: Update user group.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: name (new group name, not required), 
desription(new group description, not required), groupId.
-RETURNS: Http status 'OK'.
```
* DELETE /groups/{id}
```
-ACTION: Delete group by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-PARAMS: page - page number of messages (min value = 0), count - count messages for 1 page.
-RETURNS: Http status 'OK'.
```
* POST /groups/{id}/user/{id}/invite
```
-ACTION: Invite user to group by group id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* POST /groups/invite/{id}
```
-ACTION: Accept invite to group by invite id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* GET /groups/invites
```
-ACTION: Returns all groups invites to user.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* PATCH /groups/{id}/user/{id}/admin/add
```
-ACTION: Add group admin by group id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /groups/{id}/user/{id}/admin/delete
```
-ACTION: Delete group admin by group id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* POST /groups/{id}/user/{id}/ban
```
-ACTION: Ban user in group by group id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* DELETE /groups/{id}/user/{id}/unban
```
-ACTION: Unban user in group by group id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* DELETE /groups/{id}/user/{id}/kick
```
-ACTION: Kick user from group by group id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'
```
* DELETE /groups/{id}/leave
```
-ACTION: Leave from group by group id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* POST /groups/{id}/message
```
-ACTION: Send text message in group.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: message.
-RETURNS: Http status 'CREATED'.
```
* POST /groups/{id}/message/image
```
-ACTION: Send image to group.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .png or .jpg file.
-RETURNS: Http status 'CREATED'.
```
* POST /groups/{id}/message/video
```
-ACTION: Send video to group.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp4 file.
-RETURNS: Http status 'CREATED'.
```
* POST /groups/{id}/message/audio/ogg
```
-ACTION: Send audio ogg to group.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .ogg file.
-RETURNS: Http status 'CREATED'.
```
* POST /groups/{id}/message/audio/mp3
```
-ACTION: Send audio mp3 to group.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp3 file.
-RETURNS: Http status 'CREATED'.
```
* GET /groups/message/{id}/file
```
-ACTION: Returns message file by message id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* PATCH /groups/message/{id}
```
-ACTION: Update group message by message id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: message - updated message.
-RETURNS: Http status 'OK'
```
* DELETE /groups/message/{id}
```
-ACTION: Delete group message by message id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'
```
* PATCH /groups/{id}/image
```
-ACTION: Update group avatar be group id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .jpg or .png file.
-RETURNS: Http status 'OK'.
```
* DELETE /groups/{id}/image
```
-ACTION: Delete group image by group id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* GET /groups/{id}/image
```
-ACTION: Get group image by group id.
-BODY: 
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
**CHANNELS**
* GET /channels
```
-ACTION: Returns all user channels.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* POST /channels
```
-ACTION: Create new channel.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: name, description (not required), isPrivate (boolean, not required).
-RETURNS: Http status 'CREATED'.
```
* DELETE /channels/{id}
```
-ACTION: Delete channel by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /channels/{id}
```
-ACTION: Update channel by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: name (not required), description (not required).
```
* GET /channels/{id}
```
-ACTIONS: Returns channel by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-PARAMS: page - page number of posts (min value = 0), count - posts count for 1 page.
```
* GET /channels/find
```
-ACTIONS: Find channel by name.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: name.
```
* POST /channels/{id}/join
```
-ACTIONS: Join to channel by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* DELETE /channels/{id}/leave
```
-ACTIONS: Leave from channel by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* GET /channels/{id}/options
```
-ACTION: Returns channel options by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* PATCH /channels/{id}/options
```
-ACTION: Update channel options by id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: isPrivate (boolean), isPostsCommentsAllowed (boolean), isImagesAllowed (boolean), isInvitesAllowed (boolean), all not required.
-RETURNS: Http status 'OK'.
```
* POST /channels/{id}/user/{id}/invite
```
-ACTION: Invite user to channel by channel id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/invite/{id}
```
-ACTION: Invite by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/find
```
-ACTION: Returns channel by name.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: name. 
```
* GET /channels/{id}/image
```
-ACTION: Returns channel image by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* PATCH /channels/{id}/image
```
-ACTION: Update channel image by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .jpg or .png file.
-RETURNS: Http status 'OK'.
```
* DELETE /channels/{id}/image
```
-ACTION: Delete channel image by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* GET /channels/{id}/logs
```
-ACTION: Returns channel logs by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* POST /channels/{id}/user/{id}/ban
```
-ACTION: Ban user user in channel by channel id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* DELETE /channels/{id}/user/{id}/unban
```
-ACTION: Unban user in channel by channel id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* POST /channels/{id}/user/{id}/admin/add
```
-ACTION: Add admin to channel by channel id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.                               
```
* DELETE /channels/{id}/user/{id}/admin/delete
```
-ACTION: Delete admin by channel id and user id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* POST /channels/post
```
-ACTION: Create new post in channel.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: Text, channelId.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/{id}/post/image/
```
-ACTION: Create new post-image in channel by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .png or .jpg file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/{id}/post/video/
```
-ACTION: Create new post-video in channel by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp4 file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/{id}/post/audio/ogg
```
-ACTION: Create new post-audio.ogg in channel by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .ogg file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/{id}/post/audio/mp3
```
-ACTION: Create new post-audio.mp3 in channel by channel id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp3 file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/video
```
-ACTION: Add video to post by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp4 file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/image
```
-ACTION: Add image to post by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .png or .jpg file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/audio/ogg
```
-ACTION: Add audio.ogg to post by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .ogg file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/audio/mp3
```
-ACTION: Add audio.mp3 to post by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp3 file.
-RETURNS: Http status 'CREATED'.
```
* GET /channels/post/file/{id}
```
-ACTION: Returns post file by file id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* PATCH /channels/post
```
-ACTION: Update channel post.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: id - post id, text - new text.
-RETURNS: Http status 'OK'.
```
* DELETE /channels/post/{id}
```
-ACTION: Delete post by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* POST /channels/post/{id}/like
```
-ACTION: Add like to post by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'CREATED'.
```
* DELETE /channels/post/{id}/like
```
-ACTION: Delete like to post by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* POST /channels/post/comment
```
-ACTION: Send text comment to post.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: comment, postId.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/image
```
-ACTION: Send image comment to post.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .png or .jpg file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/comment/video
```
-ACTION: Send video comment to post.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp4 file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/audio/ogg
```
-ACTION: Send audio.ogg comment to post.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .ogg file.
-RETURNS: Http status 'CREATED'.
```
* POST /channels/post/{id}/audio/mp3
```
-ACTION: Send audio.mp3 comment to post.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: .mp3 file.
-RETURNS: Http status 'CREATED'.
```
* GET /channels/post/comment/{id}/file
```
-ACTION: Returns comment file by comment id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```
* DELETE /channels/post/comment/{id}
```
-ACTION: Delete post comment by comment id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-RETURNS: Http status 'OK'.
```
* PATCH /channels/post/comment/{id}
```
-ACTION: Update post comment by comment id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
-BODY: text.
-RETURNS: Http status 'OK'.
```
* GET /channels/post/{id}/comments
```
-ACTION: Returns all post comments by post id.
-HEADERS: 'Authorization' - 'Bearer ' + jwt token.
```