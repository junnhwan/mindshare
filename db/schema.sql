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
