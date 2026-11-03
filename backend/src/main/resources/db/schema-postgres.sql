-- DreamPet PostgreSQL (CloudBase 生产) 建表脚本，可重复执行
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pets (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL UNIQUE,
    name         VARCHAR(32) NOT NULL,
    species      VARCHAR(16) NOT NULL,
    personality  VARCHAR(16) NOT NULL,
    level        INT         NOT NULL DEFAULT 1,
    experience   INT         NOT NULL DEFAULT 0,
    growth_stage VARCHAR(16) NOT NULL DEFAULT 'seed',
    avatar_key   VARCHAR(32),
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_interests (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    interest   VARCHAR(32) NOT NULL,
    source     VARCHAR(16) NOT NULL DEFAULT 'create',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_interest UNIQUE (user_id, interest)
);
CREATE INDEX IF NOT EXISTS idx_interests_user ON user_interests (user_id);

CREATE TABLE IF NOT EXISTS tasks (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    pet_id       BIGINT       NOT NULL,
    title        VARCHAR(64)  NOT NULL,
    description  VARCHAR(500) NOT NULL,
    category     VARCHAR(16)  NOT NULL DEFAULT 'knowledge',
    difficulty   INT          NOT NULL DEFAULT 1,
    reward       INT          NOT NULL DEFAULT 10,
    status       VARCHAR(16)  NOT NULL DEFAULT 'pending',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tasks_user_status ON tasks (user_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_user_created ON tasks (user_id, created_at);

CREATE TABLE IF NOT EXISTS chat_messages (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    pet_id     BIGINT        NOT NULL,
    role       VARCHAR(16)   NOT NULL,
    content    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_pet_created ON chat_messages (pet_id, created_at);

CREATE TABLE IF NOT EXISTS memories (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    pet_id      BIGINT       NOT NULL,
    memory_type VARCHAR(16)  NOT NULL,
    content     VARCHAR(500) NOT NULL,
    importance  INT          NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_memories_user_type ON memories (user_id, memory_type);

CREATE TABLE IF NOT EXISTS growth_events (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    pet_id            BIGINT       NOT NULL,
    event_type        VARCHAR(24)  NOT NULL,
    description       VARCHAR(300) NOT NULL,
    experience_gained INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_growth_user_created ON growth_events (user_id, created_at);

CREATE TABLE IF NOT EXISTS conversation_summaries (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    pet_id        BIGINT        NOT NULL,
    summary       VARCHAR(1000) NOT NULL,
    covered_until BIGINT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_summary_pet ON conversation_summaries (pet_id);
