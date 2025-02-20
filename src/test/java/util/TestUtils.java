package util;

import danix.app.messenger_service.models.Channel;
import danix.app.messenger_service.models.Group;
import danix.app.messenger_service.models.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TestUtils {

    private TestUtils() {
    }

    public static User getTestCurrentUser() {
        User user = User.builder()
                .username("Test Username")
                .role(User.Roles.ROLE_USER)
                .email("test@gmail.com")
                .userStatus(User.Status.REGISTERED)
                .createdAt(LocalDateTime.now())
                .imageUUID("test UUID")
                .password("test_password")
                .description("test_description")
                .isPrivate(false)
                .webSocketUUID(webSocketUUID())
                .build();
        user.setId(1);
        return user;
    }

    public static User getTestUser() {
        User user = User.builder()
                .username("Username")
                .role(User.Roles.ROLE_USER)
                .email("test@gmail.com")
                .userStatus(User.Status.REGISTERED)
                .createdAt(LocalDateTime.now())
                .imageUUID("test UUID")
                .password("test_password")
                .description("test_description")
                .isPrivate(false)
                .webSocketUUID(webSocketUUID())
                .build();
        user.setId(2);
        return user;
    }

    public static Group getTestGroup() {
        return Group.builder()
                .name("test_name")
                .description("test_description")
                .createdAt(new Date())
                .webSocketUUID(webSocketUUID())
                .build();
    }

    public static Channel getTestChannel() {
        return Channel.builder()
                .name("Test name")
                .description("Test description")
                .isPrivate(false)
                .createdAt(new Date())
                .webSocketUUID(webSocketUUID())
                .build();
    }

    public static String webSocketUUID() {
        return UUID.randomUUID().toString();
    }
}
