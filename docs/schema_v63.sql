-- ============================================================================
--  「지금부터 (now)」 V6.3 — MySQL 스키마
--  테이블 22개 · 노션 「DB 설계서」(2026-08-13) 를 그대로 옮긴 것입니다
--
--  작성 2026-08-17 (PostgreSQL) · MySQL 전환 2026-08-18
--  초안 Claude · 검토·실행 송원석
--  원래 담당은 이철희 님이며, 8/17 에 송원석이 인수했습니다
--
--  ─────────────────────────────────────────────────────────────────────────
--  대상 버전 — MySQL 8.0.16 이상
--
--  ★ 8.0.15 이하 · 5.7 에서는 CHECK 제약 23개가 「조용히 무시」됩니다.
--    문법 오류가 나지 않고 그냥 안 걸립니다. 잘못된 값이 그대로 저장됩니다.
--    실행 전에 SELECT VERSION(); 으로 반드시 확인하십시오.
--
--  ─────────────────────────────────────────────────────────────────────────
--  실행 전에 읽어 주십시오
--
--  1. 「미확정」 주석이 붙은 곳은 값을 넣지 않았습니다. 임의로 채우지 마십시오
--  2. 마스터 4개(categories · care_items · signals · footsteps)는 테이블만
--     만듭니다. 시드 값은 김민정 님이 넣습니다
--  3. 이 파일은 한 번에 통째로 실행해도 되고, 섹션별로 잘라 실행해도 됩니다
--     (외래 키 순서를 맞춰 두었습니다)
--  4. 실행 후 맨 끝의 확인 쿼리로 테이블 22 · 외래키 28 을 반드시 세어 보십시오
--
--  ─────────────────────────────────────────────────────────────────────────
--  PostgreSQL 판에서 바뀐 것 — 옮길 때 조심한 곳
--
--  ① 인라인 REFERENCES 28곳을 전부 테이블 레벨 FOREIGN KEY 로 풀었습니다
--     ★ MySQL 은 컬럼 레벨 REFERENCES 를 문법만 받고 조용히 무시합니다.
--       그대로 옮겼으면 외래키가 하나도 안 생긴 채 「성공」으로 보였을 것입니다
--  ② text → VARCHAR / TEXT. MySQL 은 TEXT 를 PK·UNIQUE 로 쓸 수 없습니다
--  ③ timestamptz → DATETIME(6). ★ 시간대를 저장하지 않습니다 (아래 주의)
--  ④ now() → CURRENT_TIMESTAMP(6)
--  ⑤ jsonb → JSON
--  ⑥ 부분 인덱스 2곳의 WHERE 를 제거했습니다 — MySQL 에 없는 기능입니다
--     ★ ux_users_email 은 의미가 바뀝니다. 아래 users 주석을 보십시오
--  ⑦ rank 는 MySQL 8.0 예약어(윈도 함수)라 백틱으로 감쌌습니다
--  ⑧ text 는 컬럼명으로 쓸 때 백틱으로 감쌌습니다
--
--  ⚠️ 시간대 — DATETIME 은 시간대를 저장하지 않습니다.
--     PostgreSQL 의 timestamptz 와 다릅니다. 애플리케이션에서 KST 로 넣고
--     KST 로 읽는다는 전제입니다. 서버 시간대가 다르면 「오늘」이 어긋납니다.
--     날짜 경계는 KST 자정이고, DATE 컬럼에는 Asia/Seoul 로 계산해 넣습니다.
--  ============================================================================

-- DB 이름은 fromnowon_db 입니다.
-- ★ 리눅스 MySQL 은 DB 이름의 대소문자를 구분합니다(Windows 는 안 합니다).
--   실서버에서도 소문자 그대로 fromnowon_db 로 만드십시오.
--
-- 개발 중 다시 만들 때만 쓰십시오. 실서버에서 실행하지 마십시오.
-- DROP DATABASE IF EXISTS fromnowon_db;
-- CREATE DATABASE fromnowon_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE fromnowon_db;

-- 콜레이션은 utf8mb4_unicode_ci 입니다.
-- 8.0 기본값인 utf8mb4_0900_ai_ci 를 쓰지 않은 이유는, 만에 하나 구버전 서버가
-- 나와도 스키마 자체는 올라가게 하려는 것입니다. 한글·이모지 모두 utf8mb4 필요.

-- 문자열 길이 규칙 — 자리마다 다르게 정하지 않고 다섯 가지만 씁니다
--   VARCHAR(32)   id 계열      접두어(2) + '_' + ULID(26) = 29자 고정
--   VARCHAR(20)   코드값       keep · essential · weekly_4plus 같은 열거 문자열
--   VARCHAR(100)  짧은 이름
--   VARCHAR(255)  일반 문자열 · 해시
--   TEXT          원문 · 본문


-- ============================================================================
--  1. 인증 · 사용자
-- ============================================================================

CREATE TABLE users (
    id                     VARCHAR(32)  NOT NULL,                 -- us_ + ULID
    email                  VARCHAR(255) NULL,                     -- 게스트는 NULL
    password_hash          VARCHAR(255) NULL,                     -- 게스트는 NULL
    nickname               VARCHAR(100) NULL,
    is_guest               BOOLEAN      NOT NULL DEFAULT TRUE,
    has_seen_onboarding    BOOLEAN      NOT NULL DEFAULT FALSE,   -- S2 온보딩 1회 노출 판단
    recommendation_paused  BOOLEAN      NOT NULL DEFAULT FALSE,   -- 「오늘은 아무것도 안 할래요」
    paused_until           DATE         NULL,                     -- NULL 이면 오늘 하루만
    last_login_at          DATETIME(6)  NULL,
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at             DATETIME(6)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY ux_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 게스트도 같은 테이블에 넣습니다. 회원 전환 시 is_guest 만 false 로 바꾸면
-- 그때까지 쌓은 기록이 그대로 남습니다.
--
-- ⚠️ ux_users_email 의 의미가 PostgreSQL 판과 다릅니다.
--    원본  : UNIQUE (email) WHERE deleted_at IS NULL  — 살아 있는 회원끼리만 유일
--    MySQL : UNIQUE (email)                            — 삭제된 회원의 이메일도 계속 막습니다
--    MySQL 에는 부분 유니크 인덱스가 없습니다.
--    탈퇴 후 같은 이메일로 재가입하는 흐름이 생기면 탈퇴 시 email 을 NULL 로 비우거나
--    별도 컬럼으로 옮겨야 합니다. 지금은 탈퇴 API 가 없어 드러나지 않습니다.
--    ★ 게스트는 email 이 NULL 인데, MySQL 의 UNIQUE 는 NULL 을 여러 개 허용하므로
--      게스트가 몇 명이든 문제없습니다.


-- ⚠️ 인증 방식 — 8/18 게스트 JWT 로 구현했습니다(NOW-AUTH-001).
--    이 테이블은 세션 방식 기준이라 지금은 쓰이지 않습니다.
--    리프레시 토큰을 저장하게 되면 그때 씁니다. 지금 지우지 말고 두십시오.
CREATE TABLE sessions (
    id          VARCHAR(32)  NOT NULL,
    user_id     VARCHAR(32)  NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,                            -- 원문 저장 금지
    expires_at  DATETIME(6)  NOT NULL,
    revoked_at  DATETIME(6)  NULL,                                -- 로그아웃 시각
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY ux_sessions_token_hash (token_hash),
    KEY ix_sessions_user_expires (user_id, expires_at),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================================
--  2. 마스터 · 시드   🔒 앱에서 쓰기 없음. 배포 시 시드로 넣고 읽기만 합니다
--     테이블만 만들고 값은 김민정 님이 넣습니다
-- ============================================================================

CREATE TABLE categories (                                         -- 7행
    id          VARCHAR(32)  NOT NULL,                            -- care sleep move eat mind life med
    name        VARCHAR(100) NOT NULL,
    sort_order  SMALLINT     NOT NULL,                            -- care 가 1
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE care_items (                                         -- 32행
    id                  VARCHAR(32)  NOT NULL,                    -- cr1 sl_002 mv_001 …
    category_id         VARCHAR(32)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    floor               VARCHAR(20)  NOT NULL,
    evidence_level      VARCHAR(20)  NOT NULL,
    core                SMALLINT     NOT NULL,                    -- 중요도. 점수식 입력
    base                SMALLINT     NOT NULL,                    -- 기본 부담. 점수식 입력
    minutes             SMALLINT     NOT NULL,                    -- 고정 소요 시간(분)
    frequency_editable  BOOLEAN      NOT NULL,                    -- false 면 빈도 UI 를 띄우지 않습니다
    default_frequency   VARCHAR(20)  NULL,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_care_items_category_floor (category_id, floor),
    CONSTRAINT fk_care_items_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_care_items_floor
        CHECK (floor IN ('essential', 'recommended', 'optional', 'excluded')),
    CONSTRAINT ck_care_items_evidence
        CHECK (evidence_level IN ('high', 'medium', 'low', 'none')),
    CONSTRAINT ck_care_items_default_frequency
        CHECK (default_frequency IS NULL OR default_frequency IN
              ('weekly_1', 'weekly_2', 'weekly_3', 'weekly_4plus', 'daily'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 하한선(floor)은 데이터에서만 관리하고 코드에 상수로 박지 않습니다.
-- 판정 서버가 이 값을 읽어 LLM 결과를 검증합니다.


CREATE TABLE signals (                                            -- 14행
    id          VARCHAR(32)  NOT NULL,                            -- sig_01 … sig_14
    group_name  VARCHAR(50)  NOT NULL,                            -- 피부 수면 마음 관계 생활
    name        VARCHAR(255) NOT NULL,
    weight      SMALLINT     NOT NULL,                            -- 합계 25
    sort_order  SMALLINT     NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- group 은 예약어라 컬럼명을 group_name 으로 씁니다. API 응답 필드는 group 그대로입니다.


CREATE TABLE footsteps (                                          -- 8행
    id             VARCHAR(32)  NOT NULL,                         -- fs_101 …
    category_id    VARCHAR(32)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    who            VARCHAR(100) NOT NULL,                         -- 익명 프로필. 실명 금지
    situation      TEXT         NOT NULL,
    first_step     TEXT         NOT NULL,
    next_steps     JSON         NOT NULL,                         -- 그다음에 한 일 3건
    quote          TEXT         NOT NULL,
    is_onboarding  BOOLEAN      NOT NULL DEFAULT FALSE,           -- 온보딩 노출 4건
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_footsteps_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8건뿐이라 상세를 목록 응답에 통째로 담습니다. 별도 상세 API 를 두지 않습니다.


-- ============================================================================
--  3. 사용자 항목
-- ============================================================================

CREATE TABLE user_items (
    id            VARCHAR(32)  NOT NULL,
    user_id       VARCHAR(32)  NOT NULL,
    care_item_id  VARCHAR(32)  NULL,                              -- 직접 입력이면 NULL
    custom_name   VARCHAR(255) NULL,                              -- 직접 입력 항목명
    is_custom     BOOLEAN      NOT NULL DEFAULT FALSE,
    frequency     VARCHAR(20)  NULL,                              -- 사용자가 고른 빈도
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at    DATETIME(6)  NULL,

    PRIMARY KEY (id),
    KEY ix_user_items_user (user_id),
    CONSTRAINT fk_user_items_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_items_care_item FOREIGN KEY (care_item_id) REFERENCES care_items (id),
    CONSTRAINT ck_user_items_frequency
        CHECK (frequency IS NULL OR frequency IN
              ('weekly_1', 'weekly_2', 'weekly_3', 'weekly_4plus', 'daily')),
    CONSTRAINT ck_user_items_name
        CHECK (care_item_id IS NOT NULL OR custom_name IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ix_user_items_user 는 원본에 WHERE deleted_at IS NULL 이 붙어 있었습니다.
-- MySQL 에 부분 인덱스가 없어 뗐습니다. 삭제된 행도 인덱스에 남지만
-- 이 규모에서는 비용이 아닙니다. 조회 시 deleted_at IS NULL 을 조건에 넣으십시오.
--
-- 직접 입력 항목은 floor 가 없습니다. 판정 서버가 optional 로 취급합니다.
-- custom_name 은 저장 전 위기 신호 검사(SafetyPort)를 반드시 통과해야 합니다.
-- ⚠️ 관리 항목 최소 개수 미확정(제안값 3) — 애플리케이션에서 검증하고
--    DB 제약으로 걸지 않았습니다. 값이 바뀔 때 마이그레이션이 필요해집니다.


-- ============================================================================
--  4. 하루 사이클
-- ============================================================================

CREATE TABLE checkins (
    id              VARCHAR(32) NOT NULL,                         -- ck_ + ULID
    user_id         VARCHAR(32) NOT NULL,
    check_date      DATE        NOT NULL,                         -- KST 기준
    state           VARCHAR(20) NOT NULL,
    judge_strength  VARCHAR(20) NOT NULL,
    signal_score    SMALLINT    NOT NULL,                         -- 선택 징후 가중치 합
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY ux_checkins_user_date (user_id, check_date),       -- 하루 한 번
    CONSTRAINT fk_checkins_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_checkins_state
        CHECK (state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    CONSTRAINT ck_checkins_judge_strength
        CHECK (judge_strength IN ('low', 'medium', 'high', 'max'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- state 5종은 8/15 확정본입니다. unknown 을 빼지 마십시오 —
-- 정도의 눈금이 아니라 「답을 안 하겠다」는 선택지이고 여력값 60 이 따로 있습니다.
-- judge_strength 에 max 를 넣은 것은 drained 가 max 이기 때문입니다.


CREATE TABLE checkin_signals (
    id           VARCHAR(32)  NOT NULL,                           -- 직접 입력이 NULL 이라 복합 PK 를 쓸 수 없습니다
    checkin_id   VARCHAR(32)  NOT NULL,
    signal_id    VARCHAR(32)  NULL,                               -- 직접 입력이면 NULL
    custom_text  VARCHAR(255) NULL,                               -- 직접 적은 징후. 최대 5개
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_checkin_signals_checkin (checkin_id),
    CONSTRAINT fk_checkin_signals_checkin FOREIGN KEY (checkin_id)
        REFERENCES checkins (id) ON DELETE CASCADE,
    CONSTRAINT fk_checkin_signals_signal FOREIGN KEY (signal_id) REFERENCES signals (id),
    CONSTRAINT ck_checkin_signals_one
        CHECK (signal_id IS NOT NULL OR custom_text IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 직접 적은 징후는 하나당 가중치 2, 최대 5개입니다.
-- custom_text 도 위기 신호 검사를 통과해야 합니다.


CREATE TABLE evaluations (
    id             VARCHAR(32) NOT NULL,                          -- ev_ + ULID
    user_id        VARCHAR(32) NOT NULL,
    checkin_id     VARCHAR(32) NOT NULL,                          -- 체크 하나에 판정 하나
    state          VARCHAR(20) NOT NULL,
    judge_strength VARCHAR(20) NOT NULL,
    generated_by   VARCHAR(20) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY ux_evaluations_checkin (checkin_id),
    KEY ix_evaluations_user_created (user_id, created_at DESC),
    CONSTRAINT fk_evaluations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_evaluations_checkin FOREIGN KEY (checkin_id) REFERENCES checkins (id),
    CONSTRAINT ck_evaluations_state
        CHECK (state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    CONSTRAINT ck_evaluations_judge_strength
        CHECK (judge_strength IN ('low', 'medium', 'high', 'max')),
    CONSTRAINT ck_evaluations_generated_by
        CHECK (generated_by IN ('llm', 'fallback'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE evaluation_results (
    id             VARCHAR(32)  NOT NULL,
    evaluation_id  VARCHAR(32)  NOT NULL,
    user_item_id   VARCHAR(32)  NOT NULL,
    verdict        VARCHAR(20)  NOT NULL,
    reason         VARCHAR(500) NOT NULL,                         -- 근거 문장. 한두 문장
    evidence_level VARCHAR(20)  NOT NULL,
    floor          VARCHAR(20)  NOT NULL,                         -- 판정 시점의 하한선 (스냅숏)
    floor_applied  BOOLEAN      NOT NULL,                         -- 서버가 LLM 판정을 되돌렸으면 true
    reverted       BOOLEAN      NOT NULL DEFAULT FALSE,           -- 사용자가 되돌림
    excluded_by    VARCHAR(20)  NULL,
    note_sent      SMALLINT     NULL,                             -- 안내문 원문 문장 번호
    days_left      SMALLINT     NULL,                             -- 남은 제한 일수
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY ux_eval_results_item (evaluation_id, user_item_id),
    KEY ix_eval_results_eval (evaluation_id),
    CONSTRAINT fk_eval_results_evaluation FOREIGN KEY (evaluation_id)
        REFERENCES evaluations (id) ON DELETE CASCADE,
    CONSTRAINT fk_eval_results_user_item FOREIGN KEY (user_item_id) REFERENCES user_items (id),
    CONSTRAINT ck_eval_results_verdict
        CHECK (verdict IN ('keep', 'simplify', 'reduce', 'skip', 'excluded')),
    CONSTRAINT ck_eval_results_evidence
        CHECK (evidence_level IN ('high', 'medium', 'low', 'none')),
    CONSTRAINT ck_eval_results_floor
        CHECK (floor IN ('essential', 'recommended', 'optional', 'excluded')),
    CONSTRAINT ck_eval_results_excluded_by
        CHECK (excluded_by IS NULL OR excluded_by IN ('medical', 'clinicNote'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- verdict 5종은 8/15 확정본입니다. 프로토타입의 짧은 이름(simp red drop lock)을
-- 저장하지 마십시오.
-- floor 를 복사해 두는 이유는 마스터의 하한선이 나중에 바뀌어도
-- 그날의 판정 근거가 남아야 하기 때문입니다.
-- excluded_by 는 medical(의료 영역) 또는 clinicNote(안내문 제한)입니다.


CREATE TABLE actions (
    id                VARCHAR(32)  NOT NULL,                      -- ac_ + ULID
    user_id           VARCHAR(32)  NOT NULL,
    evaluation_id     VARCHAR(32)  NOT NULL,
    user_item_id      VARCHAR(32)  NOT NULL,                      -- 어느 항목에서 나왔는지
    title             VARCHAR(255) NOT NULL,                      -- 행동 문장
    duration_sec      INT          NOT NULL,                      -- 서버가 매번 결정. 15분 고정 아님
    status            VARCHAR(20)  NOT NULL,
    `rank`            SMALLINT     NOT NULL,                      -- 후보 순위에서 몇 번째
    total_candidates  SMALLINT     NOT NULL,                      -- 후보 총 개수
    reroll_count      SMALLINT     NOT NULL DEFAULT 0,            -- 다시 받기 횟수
    started_at        DATETIME(6)  NULL,
    completed_at      DATETIME(6)  NULL,
    expires_at        DATETIME(6)  NOT NULL,                      -- 지나면 ACTION_EXPIRED
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_actions_user_created (user_id, created_at DESC),
    CONSTRAINT fk_actions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_actions_evaluation FOREIGN KEY (evaluation_id) REFERENCES evaluations (id),
    CONSTRAINT fk_actions_user_item FOREIGN KEY (user_item_id) REFERENCES user_items (id),
    CONSTRAINT ck_actions_status
        CHECK (status IN ('pending', 'running', 'done', 'rejected'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ★ rank 는 MySQL 8.0 예약어(윈도 함수)입니다. 백틱 없이 쓰면 문법 오류가 납니다.
--   JPA 엔티티에서도 @Column(name = "`rank`") 처럼 감싸야 합니다.
--
-- 행동을 새로 만드는 곳은 GET /today 한 군데뿐입니다. 홈은 읽기 전용입니다.
-- ⚠️ 다시 받기 한도 미확정 — DB 제약으로 걸지 않았습니다.
--    초과 시 애플리케이션에서 REROLL_LIMIT 429 를 냅니다.


CREATE TABLE action_rejections (
    id           VARCHAR(32) NOT NULL,
    action_id    VARCHAR(32) NOT NULL,
    reason_code  VARCHAR(20) NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_action_rejections_action (action_id),
    CONSTRAINT fk_action_rejections_action FOREIGN KEY (action_id)
        REFERENCES actions (id) ON DELETE CASCADE,
    CONSTRAINT ck_action_rejections_reason
        CHECK (reason_code IN ('time', 'fit', 'none'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 이유가 남습니다. 「시간이 없어요」가 반복되면 제안 크기를 줄이는 쪽으로 잡습니다.
-- none 은 실패로 기록하지 않고 users.recommendation_paused 를 true 로 바꿉니다.


-- ============================================================================
--  5. 관리 맥락 · 클리닉 안내문
-- ============================================================================

CREATE TABLE care_contexts (
    user_id     VARCHAR(32) NOT NULL,                             -- 1:1
    last_type   VARCHAR(50) NULL,                                 -- 가장 최근 관리 종류
    last_date   DATE        NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    CONSTRAINT fk_care_contexts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- updated_at 에 ON UPDATE CURRENT_TIMESTAMP 를 걸지 않았습니다.
-- PostgreSQL 판과 동작을 맞추려는 것입니다. 애플리케이션에서 갱신하십시오.


CREATE TABLE care_notes (
    id           VARCHAR(32)  NOT NULL,
    user_id      VARCHAR(32)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    from_name    VARCHAR(100) NOT NULL,                           -- 발신 클리닉
    is_sample    BOOLEAN      NOT NULL DEFAULT FALSE,             -- 가상 샘플 표시
    received_at  DATE         NOT NULL,                           -- D+n 계산 기준일
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_care_notes_user (user_id),
    CONSTRAINT fk_care_notes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 클리닉 안내문 원문과 규칙은 실제 문서가 아니라 형식만 재현한 가상 샘플입니다.
-- is_sample 이 true 면 화면에 그렇게 표시해야 합니다.


CREATE TABLE care_note_lines (
    care_note_id  VARCHAR(32) NOT NULL,
    sent_no       SMALLINT    NOT NULL,                           -- 문장 번호. 신뢰 구조의 핵심
    `text`        TEXT        NOT NULL,                           -- 원문 그대로
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (care_note_id, sent_no),
    CONSTRAINT fk_care_note_lines_note FOREIGN KEY (care_note_id)
        REFERENCES care_notes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ★ text 는 데이터 타입 이름과 겹쳐 백틱으로 감쌌습니다.


CREATE TABLE care_note_rules (
    id            VARCHAR(32)  NOT NULL,
    care_note_id  VARCHAR(32)  NOT NULL,
    sent_no       SMALLINT     NOT NULL,                          -- 어느 문장에서 나온 규칙인지
    name          VARCHAR(255) NOT NULL,                          -- 「문지르는 세안」 등
    keywords      JSON         NOT NULL,                          -- 매칭용 키워드 배열
    dp            SMALLINT     NOT NULL,                          -- 제한 일수 (D+n)
    care_item_id  VARCHAR(32)  NULL,                              -- 걸리는 관리 항목
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_care_note_rules_note_dp (care_note_id, dp DESC),
    KEY ix_care_note_rules_line (care_note_id, sent_no),
    CONSTRAINT fk_care_note_rules_note FOREIGN KEY (care_note_id)
        REFERENCES care_notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_care_note_rules_care_item FOREIGN KEY (care_item_id)
        REFERENCES care_items (id),
    CONSTRAINT fk_care_note_rules_line FOREIGN KEY (care_note_id, sent_no)
        REFERENCES care_note_lines (care_note_id, sent_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 모든 주의사항이 원문 문장 번호를 답니다.
-- 「왜 이걸 하지 말라는 거지」에 원문으로 답할 수 있어야 합니다.
--
-- 남은 일수는 daysLeft = max(0, dp − 경과일) 로 계산합니다.
-- ★ 저장하지 않고 조회할 때마다 계산합니다. 저장하면 날짜가 지나도 안 줄어듭니다.
-- 여러 규칙에 걸리면 dp 가 가장 큰 것을 반환합니다.


CREATE TABLE plans (
    id          VARCHAR(32)  NOT NULL,
    user_id     VARCHAR(32)  NOT NULL,
    title       VARCHAR(255) NOT NULL,                            -- 위기 신호 검사 통과 필수
    plan_date   DATE         NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_plans_user_date (user_id, plan_date),
    CONSTRAINT fk_plans_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================================
--  6. 케어 코치 · 안전
-- ============================================================================

CREATE TABLE coach_messages (
    id            VARCHAR(32) NOT NULL,
    user_id       VARCHAR(32) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    `text`        TEXT        NOT NULL,
    cited_sents   JSON        NULL,                               -- 답변이 인용한 원문 문장 번호
    generated_by  VARCHAR(20) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_coach_messages_user_created (user_id, created_at DESC),
    CONSTRAINT fk_coach_messages_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_coach_messages_role
        CHECK (role IN ('user', 'assistant')),
    CONSTRAINT ck_coach_messages_generated_by
        CHECK (generated_by IS NULL OR generated_by IN ('rule', 'llm', 'rule+llm', 'fallback'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- generated_by 에 rule 과 rule+llm 을 넣은 것은 케어 코치가
-- 「판단은 규칙, 문장만 AI」 구조이기 때문입니다 (API 명세서 NOW-COACH-001).


CREATE TABLE safety_checks (
    id               VARCHAR(32)  NOT NULL,
    user_id          VARCHAR(32)  NULL,                           -- 비회원도 호출
    source           VARCHAR(20)  NOT NULL,
    matched          BOOLEAN      NOT NULL,                       -- 걸렸는지
    matched_keyword  VARCHAR(100) NULL,
    text_hash        VARCHAR(255) NOT NULL,                       -- ★ 원문을 저장하지 않습니다
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_safety_checks_created (created_at DESC),
    CONSTRAINT fk_safety_checks_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_safety_checks_source
        CHECK (source IN ('custom_item', 'custom_signal', 'todo', 'coach', 'care_note', 'plan'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ★ 원문을 남기지 않습니다. 위기 신호가 담긴 문장을 그대로 보관하면
--   그 자체가 위험합니다. 어떤 키워드에 걸렸는지와 해시만 남깁니다.
--
-- ⚠️ 코드의 SafetyPort.Source 열거형과 값이 다릅니다.
--    코드: ITEM_CUSTOM SIGNAL_CUSTOM COACH PLAN NOTE (5개)
--    DB  : custom_item custom_signal todo coach care_note plan (6개)
--    todo 에 대응하는 코드 값이 없습니다. 매핑을 정하고 맞춰야 합니다.
--
-- ⚠️ 위기 키워드 목록 미확정 — 현재 초안 11개. 김민정 님 확정 필요.


-- ============================================================================
--  7. 기록 · 상태 전환
-- ============================================================================

CREATE TABLE daily_logs (
    id             VARCHAR(32) NOT NULL,
    user_id        VARCHAR(32) NOT NULL,
    log_date       DATE        NOT NULL,
    state          VARCHAR(20) NULL,
    action_id      VARCHAR(32) NULL,
    done           BOOLEAN     NOT NULL DEFAULT FALSE,
    removed_count  SMALLINT    NOT NULL DEFAULT 0,                -- 그날 걷어낸 항목 수
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY ux_daily_logs_user_date (user_id, log_date),
    KEY ix_daily_logs_user_date (user_id, log_date DESC),
    CONSTRAINT fk_daily_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_daily_logs_action FOREIGN KEY (action_id) REFERENCES actions (id),
    CONSTRAINT ck_daily_logs_state
        CHECK (state IS NULL OR state IN ('energetic', 'normal', 'low', 'drained', 'unknown'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- recordedDays 는 이 테이블의 행 수입니다.
-- 7일이면 주간 리뷰, 30일이면 월간 패턴이 열립니다.
-- ⚠️ daily_logs 유지 여부 미확정 — actions.completed_at 파생안이 제안되어 있습니다.
--    확정 전에는 이 테이블을 쓰는 코드를 늘리지 마십시오.


CREATE TABLE state_transitions (
    id            VARCHAR(32) NOT NULL,
    user_id       VARCHAR(32) NOT NULL,
    from_state    VARCHAR(20) NOT NULL,
    to_state      VARCHAR(20) NOT NULL,
    signal_score  SMALLINT    NOT NULL,                           -- 제안 근거가 된 점수
    accepted      BOOLEAN     NULL,                               -- NULL 이면 아직 응답 없음
    responded_at  DATETIME(6) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_state_transitions_user_created (user_id, created_at DESC),
    CONSTRAINT fk_state_transitions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_state_transitions_from
        CHECK (from_state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    CONSTRAINT ck_state_transitions_to
        CHECK (to_state IN ('energetic', 'normal', 'low', 'drained', 'unknown'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 임계값 5 를 넘으면 전환을 제안합니다. 거절하면 유예 기간 동안 다시 묻지 않습니다.
-- ⚠️ 재제안 유예 기간 미확정(예시 3일) — created_at 으로 계산하므로 컬럼은 필요 없습니다.


-- ============================================================================
--  실행 후 확인 — ★ 이 셋을 반드시 돌리십시오
-- ============================================================================

-- ① 버전. 8.0.16 미만이면 아래 ③ 이 0 으로 나옵니다
-- SELECT VERSION();

-- ② 테이블 22개
-- SELECT COUNT(*) AS 테이블수 FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE';

-- ③ ★ 외래키 28개 — 가장 중요합니다
--    MySQL 은 컬럼 레벨 REFERENCES 를 조용히 무시하므로, 옮기다 실수하면
--    오류 없이 외래키만 사라집니다. 숫자가 28이 아니면 멈추고 원인을 찾으십시오
-- SELECT COUNT(*) AS 외래키수 FROM information_schema.TABLE_CONSTRAINTS
--  WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY';

-- ④ CHECK 제약 23개 — 8.0.15 이하면 0 입니다
-- SELECT COUNT(*) AS CHECK수 FROM information_schema.TABLE_CONSTRAINTS
--  WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'CHECK';

-- ⑤ 이름 목록으로 빠진 것 확인
-- SELECT TABLE_NAME FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
--  ORDER BY TABLE_NAME;


-- ============================================================================
--  다음에 할 일
--
--  1. 접속 정보를 김민정 님·송원석에게 먼저 공유하십시오.
--     테이블을 다 만든 뒤에 공유하면 그때까지 두 사람이 놉니다
--  2. 김민정 님이 마스터 4개(categories 7 · care_items 32 · signals 14 · footsteps 8)에
--     시드를 넣습니다. 값은 검수한 src/data/*.ts 를 씁니다
--  3. application-local.yml 에 접속 정보를 넣습니다. 커밋하지 마십시오
--  4. application.yml 의 ddl-auto 는 validate 입니다.
--     엔티티를 만들면 이 스키마와 맞는지 앱이 시작할 때 검사합니다
--     ★ actions.rank 는 엔티티에서 @Column(name = "`rank`") 로 감싸야 합니다
--
--  이 파일과 노션 「DB 설계서」가 어긋나면 노션을 고쳐 주십시오.
--  노션은 아직 PostgreSQL 기준입니다 — 갱신이 필요합니다.
-- ============================================================================
