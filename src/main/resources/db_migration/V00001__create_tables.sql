create table person
(
    id            integer generated always as identity
        primary key,
    username      varchar               not null
        unique,
    email         varchar               not null unique,
    password      varchar               not null,
    created_at    timestamp,
    role          varchar               not null,
    description   varchar,
    is_private    boolean               not null,
    status        varchar default 'REGISTERED'::character varying                           not null,
    image         varchar default '3a2cd62f-121a-48b5-b0aa-e54454d4d996'::character varying not null,
    online_status varchar default 'OFFLINE'::character varying                              not null,
    is_banned     boolean default false not null,
    web_socket_uuid varchar not null,
    last_online_status_update timestamp default CURRENT_TIMESTAMP
);

create table tokens
(
    id           varchar not null primary key,
    status       varchar not null,
    user_id      integer not null references person on delete cascade,
    expired_date date
);

create table application_messages
(
    user_id     integer not null references person on delete cascade,
    message     varchar not null,
    id          bigint generated always as identity primary key,
    sent_date   timestamp,
    remove_date timestamp
);

create table users_friends
(
    id        integer generated always as identity primary key,
    owner_id  integer
        constraint users_friends_user1_id_fkey references person on delete cascade,
    friend_id integer
        constraint users_friends_user2_id_fkey references person on delete cascade,
    status    varchar not null
);

create table banned_users
(
    id      integer generated always as identity primary key,
    user_id integer unique references person,
    reason  varchar not null
);

create table blocked_users
(
    id           integer generated always as identity primary key,
    owner_id     integer references person on delete cascade,
    blocked_user integer references person on delete cascade
);

create table email_keys
(
    id           integer generated always as identity primary key,
    email        varchar   not null unique,
    key          integer   not null,
    expired_time timestamp not null,
    attempts     integer   not null
);

create table users_chats
(
    id    integer generated always as identity primary key,
    user1 integer references person on delete cascade,
    user2 integer references person on delete cascade,
    web_socket_uuid varchar not null
);

create table chats_messages
(
    id            bigint generated always as identity primary key,
    chat          integer references users_chats on delete cascade,
    message       varchar   not null,
    message_owner integer references person on delete cascade,
    sent_time     timestamp not null,
    is_read       boolean   not null,
    content_type  varchar   not null
);

create table channels(
    id                        integer generated always as identity primary key,
    owner_id                  integer references person on delete cascade,
    created_at                date                 not null,
    is_private                boolean              not null,
    name                      varchar              not null unique,
    is_banned                 boolean              not null,
    description               varchar,
    image                     varchar default '6f03317c-1ab7-4f61-bc4e-932e36258526'::character varying not null,
    is_posts_comments_allowed boolean              not null,
    is_files_allowed          boolean default true not null,
    is_invites_allowed        boolean default true not null,
    web_socket_uuid varchar not null
);

create table channels_invites(
    id           bigint generated always as identity primary key,
    user_id      integer references person on delete cascade,
    channel_id   integer references channels on delete cascade,
    send_time    timestamp not null,
    expired_time timestamp not null
);

create table channels_logs(
    id           bigint generated always as identity primary key,
    message      varchar   not null,
    channel_id   integer references channels on delete cascade,
    created_at   timestamp not null,
    expired_time timestamp not null
);

create table channels_users(
    id         integer generated always as identity primary key,
    user_id    integer references person on delete cascade,
    channel_id integer references channels on delete cascade,
    is_admin   boolean not null
);

create table banned_channels_users
(
    user_id    integer not null references person on delete cascade,
    channel_id integer not null references channels on delete cascade,
    primary key (user_id, channel_id)
);

create table channels_posts
(
    id           bigint generated always as identity primary key,
    channel_id   integer references channels on delete cascade,
    owner_id     integer references channels_users on delete cascade,
    created_at   timestamp not null,
    content_type varchar   not null,
    post         varchar
);

create table channels_posts_comments(
    id           bigint generated always as identity primary key,
    comment      varchar   not null,
    owner_id     integer references channels_users on delete cascade,
    created_at   timestamp not null,
    post_id      integer references channels_posts on delete cascade,
    content_type varchar   not null
);

create table channels_posts_files(
    id           bigint generated always as identity constraint channels_posts_images_pkey primary key,
    file_uuid    varchar not null constraint channels_posts_images_image_uuid_key unique,
    post_id      bigint  not null constraint channels_posts_images_post_id_fkey references channels_posts on delete cascade,
    content_type varchar not null
);

create table channels_posts_likes(
    user_id integer not null references person on delete cascade,
    post_id integer not null references channels_posts  on delete cascade,
    primary key (user_id, post_id)
);

create table groups(
    id          integer generated always as identity primary key,
    name        varchar not null,
    owner_id    integer references person on delete cascade,
    created_at  date not null,
    description varchar,
    image       varchar default '393de5ef-bd11-4057-9863-77d49c47c806'::character varying not null,
    web_socket_uuid varchar not null
);

create table group_users(
    id       integer generated always as identity primary key,
    user_id  integer references person on delete cascade,
    group_id integer references groups on delete cascade,
    is_admin boolean not null
);

create table groups_banned_users(
    user_id  integer not null references person on delete cascade,
    group_id integer not null references groups on delete cascade,
    primary key (user_id, group_id)
);

create table groups_invites(
    id           integer generated always as identity primary key,
    user_id      integer   not null references person on delete cascade,
    group_id     integer   not null references groups on delete cascade,
    sent_time    timestamp not null,
    expired_time timestamp not null
);

create table groups_actions_messages(
    id        bigint generated always as identity primary key,
    message   varchar   not null,
    group_id  integer references groups on delete cascade,
    sent_time timestamp not null
);

create table groups_messages(
    message       varchar   not null,
    message_owner integer references person on delete cascade,
    group_id      integer   not null references groups on delete cascade,
    id            bigint generated always as identity primary key,
    sent_time     timestamp not null,
    content_type  varchar   not null
);