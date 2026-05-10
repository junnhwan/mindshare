CREATE TABLE IF NOT EXISTS schema_version (
    version_num INT NOT NULL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(32),
    email VARCHAR(128),
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar VARCHAR(512),
    bio VARCHAR(512),
    zg_id VARCHAR(64),
    tags_json TEXT,
    birthday DATE,
    school VARCHAR(128),
    gender VARCHAR(32),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone ON users(phone);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users(email);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_zg_id ON users(zg_id);

CREATE TABLE IF NOT EXISTS login_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    identifier VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    result VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS know_posts (
    id BIGINT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    visible VARCHAR(32) NOT NULL,
    is_top BOOLEAN NOT NULL DEFAULT FALSE,
    tag_id BIGINT,
    title VARCHAR(255),
    description VARCHAR(512),
    tags TEXT,
    img_urls TEXT,
    video_url VARCHAR(512),
    content_object_key VARCHAR(512),
    content_etag VARCHAR(255),
    content_size BIGINT,
    content_sha256 VARCHAR(128),
    content_url VARCHAR(512),
    publish_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_know_posts_creator_id FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_know_posts_creator_id ON know_posts(creator_id);
CREATE INDEX IF NOT EXISTS idx_know_posts_status_visible_publish_time
    ON know_posts(status, visible, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_know_posts_creator_ct ON know_posts(creator_id, create_time);
CREATE INDEX IF NOT EXISTS idx_know_posts_status_ct ON know_posts(status, create_time);
CREATE INDEX IF NOT EXISTS idx_know_posts_creator_status_pub ON know_posts(creator_id, status, publish_time);

CREATE TABLE IF NOT EXISTS outbox (
    id BIGINT PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_agg ON outbox(aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_ct ON outbox(created_at);

CREATE TABLE IF NOT EXISTS following (
    id BIGINT PRIMARY KEY,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    rel_status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_from_to UNIQUE (from_user_id, to_user_id)
);

CREATE INDEX IF NOT EXISTS idx_following_from ON following(from_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_following_to ON following(to_user_id, from_user_id);

CREATE TABLE IF NOT EXISTS follower (
    id BIGINT PRIMARY KEY,
    to_user_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    rel_status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_to_from UNIQUE (to_user_id, from_user_id)
);

CREATE INDEX IF NOT EXISTS idx_follower_to ON follower(to_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_follower_from ON follower(from_user_id, to_user_id);
