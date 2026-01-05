/*
===========================================
  🚨 테이블 생성 순서가 중요한 이유
-------------------------------------------
1. 외래 키(FK)는 다른 테이블을 참조하므로,
    참조 대상이 먼저 생성되어 있어야 한다.
    예) feed_clothes → clothes / feeds

2. 따라서 기본 엔티티(users, clothes, locations 등)
    → 종속 엔티티(user_profiles, feeds 등)
    → N:M 관계 테이블(feed_clothes 등)
    순서로 정의해야 오류 없이 생성된다.
===========================================
*/

/**
============= DROP (초기화용) =============
*/
-- Postgres에서 필요시 활성화, 테스트/H2에서는 별도 관리
-- DROP TABLE IF EXISTS feed_clothes CASCADE;
-- DROP TABLE IF EXISTS clothes_attributes CASCADE;
-- DROP TABLE IF EXISTS clothes_attribute_options CASCADE;
-- DROP TABLE IF EXISTS clothes_attribute_defs CASCADE;
-- DROP TABLE IF EXISTS feed_comments CASCADE;
-- DROP TABLE IF EXISTS feed_likes CASCADE;
-- DROP TABLE IF EXISTS recommendation_clothes CASCADE;
-- DROP TABLE IF EXISTS recommendation CASCADE;
-- DROP TABLE IF EXISTS direct_messages CASCADE;
-- DROP TABLE IF EXISTS follows CASCADE;
-- DROP TABLE IF EXISTS weather_data CASCADE;
-- DROP TABLE IF EXISTS notifications CASCADE;
-- DROP TABLE IF EXISTS user_profiles CASCADE;
-- DROP TABLE IF EXISTS feeds CASCADE;
-- DROP TABLE IF EXISTS clothes CASCADE;
-- DROP TABLE IF EXISTS locations CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;

/**
============= 사용자, 위치 정보 =============
 */
CREATE TABLE IF NOT EXISTS users
(
    id                                 uuid PRIMARY KEY,
    provider                           varchar(20),
    provider_user_id                   varchar(255),
    email                              varchar(255)             NOT NULL UNIQUE,
    password                           varchar(255)             NOT NULL,
    temporary_password                 varchar(255),
    temporary_password_expiration_time TIMESTAMP WITH TIME ZONE,
    role                               varchar(20)              NOT NULL DEFAULT 'USER',
    locked                             boolean                  NOT NULL DEFAULT false,
    created_at                         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                         TIMESTAMP WITH TIME ZONE,
    CHECK (provider IN ('GOOGLE', 'KAKAO')),
    CHECK (role IN ('USER', 'ADMIN')),
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE IF NOT EXISTS locations
(
    id             uuid PRIMARY KEY,
    latitude       double precision NOT NULL,
    longitude      double precision NOT NULL,
    x_coordinate   integer          NOT NULL,
    y_coordinate   integer          NOT NULL,
    location_names varchar(255)     NOT NULL,
    CHECK (latitude BETWEEN -90 AND 90),
    CHECK (longitude BETWEEN -180 AND 180),
    UNIQUE (latitude, longitude)
);

CREATE TABLE IF NOT EXISTS user_profiles
(
    id                uuid PRIMARY KEY,
    user_id           uuid                     NOT NULL,
    location_id       uuid,
    nickname          varchar(100)             NOT NULL,
    profile_image_url varchar(255),
    gender            varchar(20),
    birth_date        date,
    temp_sensitivity  integer,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE,
    UNIQUE (user_id),
    CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CHECK (temp_sensitivity BETWEEN 1 AND 5),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE SET NULL
);

/**
============= 옷장 =============
 */
CREATE TABLE IF NOT EXISTS clothes
(
    id         uuid PRIMARY KEY,
    owner_id   uuid                     NOT NULL,
    name       varchar(255),
    type       varchar(20),
    image_url  text,
    feed_count bigint                   NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CHECK (type IN
           ('TOP', 'BOTTOM', 'DRESS', 'OUTER', 'UNDERWEAR', 'ACCESSORY', 'SHOES', 'SOCKS', 'HAT',
            'BAG', 'SCARF', 'ETC'))
);

CREATE TABLE IF NOT EXISTS clothes_attribute_defs
(
    id         uuid PRIMARY KEY,
    name       varchar(100)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS clothes_attribute_options
(
    id            uuid PRIMARY KEY,
    option_value  varchar(50) NOT NULL,
    definition_id uuid        NOT NULL,
    FOREIGN KEY (definition_id) REFERENCES clothes_attribute_defs (id) ON DELETE CASCADE,
    UNIQUE (definition_id, option_value)
);

CREATE TABLE IF NOT EXISTS clothes_attributes
(
    id            uuid PRIMARY KEY,
    clothes_id    uuid                     NOT NULL,
    definition_id uuid                     NOT NULL,
    option_value  varchar(50)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE,
    FOREIGN KEY (definition_id) REFERENCES clothes_attribute_defs (id) ON DELETE CASCADE,
    UNIQUE (clothes_id, definition_id)
);

/**
============= 날씨 데이터 =============
 */
CREATE TABLE IF NOT EXISTS weather_data
(
    id                                 uuid PRIMARY KEY,
    location_id                        uuid                     NOT NULL,
    forecasted_at                      TIMESTAMP WITH TIME ZONE,
    forecast_at                        TIMESTAMP WITH TIME ZONE,
    temperature_current                double precision         NOT NULL,
    temperature_max                    double precision,
    temperature_min                    double precision,
    temperature_compared_to_day_before double precision,
    sky_status                         varchar(16),
    precipitation_amount               double precision,
    precipitation_probability          double precision,
    precipitation_type                 varchar(16),
    wind_speed                         double precision,
    wind_speed_as_word                 varchar(16),
    humidity                           double precision,
    humidity_compared_to_day_before    double precision,
    created_at                         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE CASCADE,
    CHECK (sky_status IN ('CLEAR', 'MOSTLY_CLOUDY', 'CLOUDY')),
    CHECK (precipitation_type IN ('NONE', 'RAIN', 'RAIN_SNOW', 'SNOW', 'SHOWER')),
    CHECK (wind_speed_as_word IN ('WEAK', 'MODERATE', 'STRONG'))
);

/*
============= FEED & SOCIAL (피드/댓글/좋아요/팔로우/DM) =============
 */
CREATE TABLE IF NOT EXISTS feeds
(
    id            uuid PRIMARY KEY,
    author_id     uuid                     NOT NULL,
    weather_id    uuid,
    content       varchar(1000),
    comment_count bigint                   NOT NULL DEFAULT 0,
    like_count    bigint                   NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (weather_id) REFERENCES weather_data (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS feed_comments
(
    id         uuid PRIMARY KEY,
    author_id  uuid                     NOT NULL,
    feed_id    uuid                     NOT NULL,
    content    varchar(200)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feed_likes
(
    id         uuid PRIMARY KEY,
    user_id    uuid                     NOT NULL,
    feed_id    uuid                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (user_id, feed_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feed_clothes
(
    id         uuid PRIMARY KEY,
    feed_id    uuid                     NOT NULL,
    clothes_id uuid                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (feed_id, clothes_id),
    FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE,
    FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS follows
(
    id           uuid PRIMARY KEY,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    follower_id  uuid                     NOT NULL,
    following_id uuid                     NOT NULL,
    UNIQUE (follower_id, following_id),
    CHECK (follower_id <> following_id),
    FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS direct_messages
(
    id          uuid PRIMARY KEY,
    content     text                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    receiver_id uuid                     NOT NULL,
    sender_id   uuid                     NOT NULL,
    FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE
);

/*
============= RECOMMENDATION & NOTI (추천/알림) =============
 */
CREATE TABLE IF NOT EXISTS recommendation
(
    id         uuid PRIMARY KEY,
    user_id    uuid                     NOT NULL,
    weather_id uuid                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (weather_id) REFERENCES weather_data (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS recommendation_clothes
(
    id                uuid PRIMARY KEY,
    clothes_id        uuid NOT NULL,
    recommendation_id uuid NOT NULL,
    UNIQUE (recommendation_id, clothes_id),
    FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE,
    FOREIGN KEY (recommendation_id) REFERENCES recommendation (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS recommendation_parameter
(
    id                uuid PRIMARY KEY,
    recommendation_id uuid                     NOT NULL,
    season_int        int,
    gender_int        int,
    age               int,
    temp_sens         int,
    sky_status_int    int,
    min_temp          double precision,
    max_temp          double precision,
    cur_temp          double precision,
    humidity          double precision,
    wind_speed        double precision,
    feel_hot          double precision,
    feel_cold         double precision,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (recommendation_id) REFERENCES recommendation (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications
(
    id          uuid PRIMARY KEY,
    level       varchar(20)              NOT NULL,
    title       varchar(255)             NOT NULL,
    content     varchar(255)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    receiver_id uuid                     NOT NULL,
    FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE,
    CHECK (level IN ('INFO', 'WARNING', 'ERROR'))
);

CREATE TABLE IF NOT EXISTS weather_alert_outboxes
(
    id          uuid PRIMARY KEY,
    location_id UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    type        VARCHAR(50)              NOT NULL,
    alert_date  DATE                     NOT NULL,
    title       VARCHAR(255)             NOT NULL,
    message     TEXT                     NOT NULL,
    status      VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE CASCADE,
    CHECK (status IN ('PENDING', 'SENDING', 'SEND', 'FAILED')),
    CHECK (type IN ('TEMPERATURE_CHANGE', 'WIND_CHANGE', 'PRECIPITATION_CHANGE'))
);

/*
============= INDEX (조회 성능 최적화용) =============
 */
CREATE INDEX IF NOT EXISTS idx_dm_receiver_created_at ON direct_messages (receiver_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dm_sender_created_at ON direct_messages (sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feed_comments_feed_created_at ON feed_comments (feed_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feed_likes_feed ON feed_likes (feed_id);
CREATE INDEX IF NOT EXISTS idx_feed_author_created_at ON feeds (author_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_weather_location_time ON weather_data (location_id, forecast_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_weather_alert_outboxes_dedup
    ON weather_alert_outboxes (user_id, location_id, type, alert_date);

/*
    Batch Job을 커스터마이징하게 되면 batch.jdbc.initialize-schema=always 설정이 동작하지 않을 수 있다.
    그땐 직접 Batch Core Tables를 생성해주어 Batch Job을 정상 동작할 수 있게 한다.
    https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-db2.sql
    단, PostgreSQL의 타입과 맞지 않는 부분은 수정이 필요하다.
 */
CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE
(
    JOB_INSTANCE_ID BIGINT       NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100) NOT NULL,
    JOB_KEY         VARCHAR(32)  NOT NULL,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION
(
    JOB_EXECUTION_ID BIGINT    NOT NULL PRIMARY KEY,
    VERSION          BIGINT,
    JOB_INSTANCE_ID  BIGINT    NOT NULL,
    CREATE_TIME      TIMESTAMP NOT NULL,
    START_TIME       TIMESTAMP DEFAULT NULL,
    END_TIME         TIMESTAMP DEFAULT NULL,
    STATUS           VARCHAR(10),
    EXIT_CODE        VARCHAR(2500),
    EXIT_MESSAGE     VARCHAR(2500),
    LAST_UPDATED     TIMESTAMP,
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
        references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS
(
    JOB_EXECUTION_ID BIGINT       NOT NULL,
    PARAMETER_NAME   VARCHAR(100) NOT NULL,
    PARAMETER_TYPE   VARCHAR(100) NOT NULL,
    PARAMETER_VALUE  VARCHAR(2500),
    IDENTIFYING      CHAR(1)      NOT NULL,
    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION
(
    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    CREATE_TIME        TIMESTAMP    NOT NULL,
    START_TIME         TIMESTAMP DEFAULT NULL,
    END_TIME           TIMESTAMP DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    WRITE_SKIP_COUNT   BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       TIMESTAMP,
    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT
(
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
        references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT
(
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE SEQUENCE IF NOT EXISTS BATCH_STEP_EXECUTION_SEQ AS BIGINT MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_EXECUTION_SEQ AS BIGINT MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_SEQ AS BIGINT MAXVALUE 9223372036854775807 NO CYCLE;
CREATE INDEX IF NOT EXISTS idx_weather_alert_outboxes_location_id ON weather_alert_outboxes (location_id);
CREATE INDEX IF NOT EXISTS idx_weather_alert_outboxes_status ON weather_alert_outboxes (status);
