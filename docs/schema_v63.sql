-- ============================================================================
--  「지금부터 (now)」 V6.3 — PostgreSQL 스키마
--  테이블 22개 · 노션 「DB 설계서」(2026-08-13) 를 그대로 옮긴 것입니다
--
--  작성 2026-08-17 · 초안 Claude · 검토·실행 송원석
--  원래 담당은 이철희 님이며, 8/17 에 송원석이 인수했습니다
--
--  ─────────────────────────────────────────────────────────────────────────
--  실행 전에 읽어 주십시오
--
--  1. 「미확정」 주석이 붙은 곳은 값을 넣지 않았습니다. 임의로 채우지 마십시오
--  2. 마스터 4개(categories · care_items · signals · footsteps)는 테이블만
--     만듭니다. 시드 값은 김민정 님이 넣습니다
--  3. 이 파일은 한 번에 통째로 실행해도 되고, 섹션별로 잘라 실행해도 됩니다
--     (외래 키 순서를 맞춰 두었습니다)
--  4. 실행 후 아래 맨 끝의 확인 쿼리로 22개가 다 생겼는지 보십시오
--  ============================================================================

-- 개발 중 다시 만들 때만 쓰십시오. 실서버에서 실행하지 마십시오.
-- DROP SCHEMA public CASCADE; CREATE SCHEMA public;

SET client_encoding = 'UTF8';

-- 날짜 경계는 KST 자정입니다. 「오늘」이 사용자 체감과 맞아야 합니다.
-- 애플리케이션에서 Asia/Seoul 로 계산해 DATE 컬럼에 넣습니다.


-- ============================================================================
--  1. 인증 · 사용자
-- ============================================================================

CREATE TABLE users (
    id                     text        PRIMARY KEY,              -- us_ + ULID
    email                  text        NULL,                     -- 게스트는 NULL
    password_hash          text        NULL,                     -- 게스트는 NULL
    nickname               text        NULL,
    is_guest               boolean     NOT NULL DEFAULT true,
    has_seen_onboarding    boolean     NOT NULL DEFAULT false,   -- S2 온보딩 1회 노출 판단
    recommendation_paused  boolean     NOT NULL DEFAULT false,   -- 「오늘은 아무것도 안 할래요」
    paused_until           date        NULL,                     -- NULL 이면 오늘 하루만
    last_login_at          timestamptz NULL,
    created_at             timestamptz NOT NULL DEFAULT now(),
    deleted_at             timestamptz NULL
);

-- 게스트도 같은 테이블에 넣습니다. 회원 전환 시 is_guest 만 false 로 바꾸면
-- 그때까지 쌓은 기록이 그대로 남습니다.
CREATE UNIQUE INDEX ux_users_email ON users (email) WHERE deleted_at IS NULL;


-- ⚠️ 인증 방식 미확정 — 이 테이블은 세션 방식 기준입니다.
--    JWT 로 가면 리프레시 토큰 저장용으로만 남습니다. 그 경우 이 테이블을
--    안 만드셔도 되지만, 나중에 붙이는 것보다 지금 두는 편이 쌉니다.
CREATE TABLE sessions (
    id          text        PRIMARY KEY,
    user_id     text        NOT NULL REFERENCES users(id),
    token_hash  text        NOT NULL UNIQUE,                     -- 원문 저장 금지
    expires_at  timestamptz NOT NULL,
    revoked_at  timestamptz NULL,                                -- 로그아웃 시각
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_sessions_user_expires ON sessions (user_id, expires_at);


-- ============================================================================
--  2. 마스터 · 시드   🔒 앱에서 쓰기 없음. 배포 시 시드로 넣고 읽기만 합니다
--     테이블만 만들고 값은 김민정 님이 넣습니다
-- ============================================================================

CREATE TABLE categories (                                        -- 7행
    id          text     PRIMARY KEY,                            -- care sleep move eat mind life med
    name        text     NOT NULL,
    sort_order  smallint NOT NULL,                               -- care 가 1
    created_at  timestamptz NOT NULL DEFAULT now()
);


CREATE TABLE care_items (                                        -- 32행
    id                  text     PRIMARY KEY,                    -- cr1 sl_002 mv_001 …
    category_id         text     NOT NULL REFERENCES categories(id),
    name                text     NOT NULL,
    floor               text     NOT NULL
        CHECK (floor IN ('essential', 'recommended', 'optional', 'excluded')),
    evidence_level      text     NOT NULL
        CHECK (evidence_level IN ('high', 'medium', 'low', 'none')),
    core                smallint NOT NULL,                       -- 중요도. 점수식 입력
    base                smallint NOT NULL,                       -- 기본 부담. 점수식 입력
    minutes             smallint NOT NULL,                       -- 고정 소요 시간(분)
    frequency_editable  boolean  NOT NULL,                       -- false 면 빈도 UI 를 띄우지 않습니다
    default_frequency   text     NULL
        CHECK (default_frequency IS NULL OR default_frequency IN
              ('weekly_1', 'weekly_2', 'weekly_3', 'weekly_4plus', 'daily')),
    created_at          timestamptz NOT NULL DEFAULT now()
);

-- 하한선(floor)은 데이터에서만 관리하고 코드에 상수로 박지 않습니다.
-- 판정 서버가 이 값을 읽어 LLM 결과를 검증합니다.
CREATE INDEX ix_care_items_category_floor ON care_items (category_id, floor);


CREATE TABLE signals (                                           -- 14행
    id          text     PRIMARY KEY,                            -- sig_01 … sig_14
    group_name  text     NOT NULL,                               -- 피부 수면 마음 관계 생활
    name        text     NOT NULL,
    weight      smallint NOT NULL,                               -- 합계 25
    sort_order  smallint NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

-- group 은 PostgreSQL 예약어라 컬럼명을 group_name 으로 씁니다.
-- API 응답 필드는 group 그대로입니다.


CREATE TABLE footsteps (                                         -- 8행
    id             text    PRIMARY KEY,                          -- fs_101 …
    category_id    text    NOT NULL REFERENCES categories(id),
    title          text    NOT NULL,
    who            text    NOT NULL,                             -- 익명 프로필. 실명 금지
    situation      text    NOT NULL,
    first_step     text    NOT NULL,
    next_steps     jsonb   NOT NULL,                             -- 그다음에 한 일 3건
    quote          text    NOT NULL,
    is_onboarding  boolean NOT NULL DEFAULT false,               -- 온보딩 노출 4건
    created_at     timestamptz NOT NULL DEFAULT now()
);

-- 8건뿐이라 상세를 목록 응답에 통째로 담습니다. 별도 상세 API 를 두지 않습니다.


-- ============================================================================
--  3. 사용자 항목
-- ============================================================================

CREATE TABLE user_items (
    id            text        PRIMARY KEY,
    user_id       text        NOT NULL REFERENCES users(id),
    care_item_id  text        NULL REFERENCES care_items(id),    -- 직접 입력이면 NULL
    custom_name   text        NULL,                              -- 직접 입력 항목명
    is_custom     boolean     NOT NULL DEFAULT false,
    frequency     text        NULL                               -- 사용자가 고른 빈도
        CHECK (frequency IS NULL OR frequency IN
              ('weekly_1', 'weekly_2', 'weekly_3', 'weekly_4plus', 'daily')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz NULL,

    CONSTRAINT ck_user_items_name
        CHECK (care_item_id IS NOT NULL OR custom_name IS NOT NULL)
);

CREATE INDEX ix_user_items_user ON user_items (user_id) WHERE deleted_at IS NULL;

-- 직접 입력 항목은 floor 가 없습니다. 판정 서버가 optional 로 취급합니다.
-- custom_name 은 저장 전 위기 신호 검사(SafetyPort)를 반드시 통과해야 합니다.
-- ⚠️ 관리 항목 최소 개수 미확정(제안값 3) — 애플리케이션에서 검증하고
--    DB 제약으로 걸지 않았습니다. 값이 바뀔 때 마이그레이션이 필요해집니다.


-- ============================================================================
--  4. 하루 사이클
-- ============================================================================

CREATE TABLE checkins (
    id              text        PRIMARY KEY,                     -- ck_ + ULID
    user_id         text        NOT NULL REFERENCES users(id),
    check_date      date        NOT NULL,                        -- KST 기준
    state           text        NOT NULL
        CHECK (state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    judge_strength  text        NOT NULL
        CHECK (judge_strength IN ('low', 'medium', 'high', 'max')),
    signal_score    smallint    NOT NULL,                        -- 선택 징후 가중치 합
    created_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ux_checkins_user_date UNIQUE (user_id, check_date)   -- 하루 한 번
);

-- state 5종은 8/15 확정본입니다. unknown 을 빼지 마십시오 —
-- 정도의 눈금이 아니라 「답을 안 하겠다」는 선택지이고 여력값 60 이 따로 있습니다.
-- judge_strength 에 max 를 넣은 것은 drained 가 max 이기 때문입니다.
-- (DB 설계서 본문에는 low/medium/high 만 적혀 있으나 API 명세서에 max 가 있습니다)


CREATE TABLE checkin_signals (
    id           text        PRIMARY KEY,                        -- 직접 입력이 NULL 이라 복합 PK 를 쓸 수 없습니다
    checkin_id   text        NOT NULL REFERENCES checkins(id) ON DELETE CASCADE,
    signal_id    text        NULL REFERENCES signals(id),        -- 직접 입력이면 NULL
    custom_text  text        NULL,                               -- 직접 적은 징후. 최대 5개
    created_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ck_checkin_signals_one
        CHECK (signal_id IS NOT NULL OR custom_text IS NOT NULL)
);

CREATE INDEX ix_checkin_signals_checkin ON checkin_signals (checkin_id);

-- 직접 적은 징후는 하나당 가중치 2, 최대 5개입니다.
-- custom_text 도 위기 신호 검사를 통과해야 합니다.


CREATE TABLE evaluations (
    id            text        PRIMARY KEY,                       -- ev_ + ULID
    user_id       text        NOT NULL REFERENCES users(id),
    checkin_id    text        NOT NULL UNIQUE REFERENCES checkins(id),  -- 체크 하나에 판정 하나
    state         text        NOT NULL
        CHECK (state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    judge_strength text       NOT NULL
        CHECK (judge_strength IN ('low', 'medium', 'high', 'max')),
    generated_by  text        NOT NULL
        CHECK (generated_by IN ('llm', 'fallback')),
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_evaluations_user_created ON evaluations (user_id, created_at DESC);


CREATE TABLE evaluation_results (
    id             text        PRIMARY KEY,
    evaluation_id  text        NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    user_item_id   text        NOT NULL REFERENCES user_items(id),
    verdict        text        NOT NULL
        CHECK (verdict IN ('keep', 'simplify', 'reduce', 'skip', 'excluded')),
    reason         text        NOT NULL,                         -- 근거 문장
    evidence_level text        NOT NULL
        CHECK (evidence_level IN ('high', 'medium', 'low', 'none')),
    floor          text        NOT NULL                          -- 판정 시점의 하한선 (스냅숏)
        CHECK (floor IN ('essential', 'recommended', 'optional', 'excluded')),
    floor_applied  boolean     NOT NULL,                         -- 서버가 LLM 판정을 되돌렸으면 true
    reverted       boolean     NOT NULL DEFAULT false,           -- 사용자가 되돌림
    excluded_by    text        NULL
        CHECK (excluded_by IS NULL OR excluded_by IN ('medical', 'clinicNote')),
    note_sent      smallint    NULL,                             -- 안내문 원문 문장 번호
    days_left      smallint    NULL,                             -- 남은 제한 일수
    created_at     timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ux_eval_results_item UNIQUE (evaluation_id, user_item_id)
);

CREATE INDEX ix_eval_results_eval ON evaluation_results (evaluation_id);

-- verdict 5종은 8/15 확정본입니다. 프로토타입의 짧은 이름(simp red drop lock)을
-- 저장하지 마십시오.
-- floor 를 복사해 두는 이유는 마스터의 하한선이 나중에 바뀌어도
-- 그날의 판정 근거가 남아야 하기 때문입니다.
-- excluded_by 는 medical(의료 영역) 또는 clinicNote(안내문 제한)입니다.


CREATE TABLE actions (
    id                text        PRIMARY KEY,                   -- ac_ + ULID
    user_id           text        NOT NULL REFERENCES users(id),
    evaluation_id     text        NOT NULL REFERENCES evaluations(id),
    user_item_id      text        NOT NULL REFERENCES user_items(id),  -- 어느 항목에서 나왔는지
    title             text        NOT NULL,                      -- 행동 문장
    duration_sec      integer     NOT NULL,                      -- 서버가 매번 결정. 15분 고정 아님
    status            text        NOT NULL
        CHECK (status IN ('pending', 'running', 'done', 'rejected')),
    rank              smallint    NOT NULL,                      -- 후보 순위에서 몇 번째
    total_candidates  smallint    NOT NULL,                      -- 후보 총 개수
    reroll_count      smallint    NOT NULL DEFAULT 0,            -- 다시 받기 횟수
    started_at        timestamptz NULL,
    completed_at      timestamptz NULL,
    expires_at        timestamptz NOT NULL,                      -- 지나면 ACTION_EXPIRED
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_actions_user_created ON actions (user_id, created_at DESC);

-- 행동을 새로 만드는 곳은 GET /today 한 군데뿐입니다. 홈은 읽기 전용입니다.
-- ⚠️ 다시 받기 한도 미확정 — DB 제약으로 걸지 않았습니다.
--    초과 시 애플리케이션에서 REROLL_LIMIT 429 를 냅니다.


CREATE TABLE action_rejections (
    id           text        PRIMARY KEY,
    action_id    text        NOT NULL REFERENCES actions(id) ON DELETE CASCADE,
    reason_code  text        NOT NULL
        CHECK (reason_code IN ('time', 'fit', 'none')),
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_action_rejections_action ON action_rejections (action_id);

-- 이유가 남습니다. 「시간이 없어요」가 반복되면 제안 크기를 줄이는 쪽으로 잡습니다.
-- none 은 실패로 기록하지 않고 users.recommendation_paused 를 true 로 바꿉니다.


-- ============================================================================
--  5. 관리 맥락 · 클리닉 안내문
-- ============================================================================

CREATE TABLE care_contexts (
    user_id     text        PRIMARY KEY REFERENCES users(id),    -- 1:1
    last_type   text        NULL,                                -- 가장 최근 관리 종류
    last_date   date        NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);


CREATE TABLE care_notes (
    id           text        PRIMARY KEY,
    user_id      text        NOT NULL REFERENCES users(id),
    title        text        NOT NULL,
    from_name    text        NOT NULL,                           -- 발신 클리닉
    is_sample    boolean     NOT NULL DEFAULT false,             -- 가상 샘플 표시
    received_at  date        NOT NULL,                           -- D+n 계산 기준일
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_care_notes_user ON care_notes (user_id);

-- 클리닉 안내문 원문과 규칙은 실제 문서가 아니라 형식만 재현한 가상 샘플입니다.
-- is_sample 이 true 면 화면에 그렇게 표시해야 합니다.


CREATE TABLE care_note_lines (
    care_note_id  text        NOT NULL REFERENCES care_notes(id) ON DELETE CASCADE,
    sent_no       smallint    NOT NULL,                          -- 문장 번호. 신뢰 구조의 핵심
    text          text        NOT NULL,                          -- 원문 그대로
    created_at    timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (care_note_id, sent_no)
);


CREATE TABLE care_note_rules (
    id            text        PRIMARY KEY,
    care_note_id  text        NOT NULL REFERENCES care_notes(id) ON DELETE CASCADE,
    sent_no       smallint    NOT NULL,                          -- 어느 문장에서 나온 규칙인지
    name          text        NOT NULL,                          -- 「문지르는 세안」 등
    keywords      jsonb       NOT NULL,                          -- 매칭용 키워드 배열
    dp            smallint    NOT NULL,                          -- 제한 일수 (D+n)
    care_item_id  text        NULL REFERENCES care_items(id),    -- 걸리는 관리 항목
    created_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_care_note_rules_line
        FOREIGN KEY (care_note_id, sent_no) REFERENCES care_note_lines(care_note_id, sent_no)
);

CREATE INDEX ix_care_note_rules_note_dp ON care_note_rules (care_note_id, dp DESC);

-- 모든 주의사항이 원문 문장 번호를 답니다.
-- 「왜 이걸 하지 말라는 거지」에 원문으로 답할 수 있어야 합니다.
--
-- 남은 일수는 daysLeft = max(0, dp − 경과일) 로 계산합니다.
-- ★ 저장하지 않고 조회할 때마다 계산합니다. 저장하면 날짜가 지나도 안 줄어듭니다.
-- 여러 규칙에 걸리면 dp 가 가장 큰 것을 반환합니다.


CREATE TABLE plans (
    id          text        PRIMARY KEY,
    user_id     text        NOT NULL REFERENCES users(id),
    title       text        NOT NULL,                            -- 위기 신호 검사 통과 필수
    plan_date   date        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_plans_user_date ON plans (user_id, plan_date);


-- ============================================================================
--  6. 케어 코치 · 안전
-- ============================================================================

CREATE TABLE coach_messages (
    id            text        PRIMARY KEY,
    user_id       text        NOT NULL REFERENCES users(id),
    role          text        NOT NULL
        CHECK (role IN ('user', 'assistant')),
    text          text        NOT NULL,
    cited_sents   jsonb       NULL,                              -- 답변이 인용한 원문 문장 번호
    generated_by  text        NULL
        CHECK (generated_by IS NULL OR generated_by IN ('rule', 'llm', 'rule+llm', 'fallback')),
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_coach_messages_user_created ON coach_messages (user_id, created_at DESC);

-- generated_by 에 rule 과 rule+llm 을 넣은 것은 케어 코치가
-- 「판단은 규칙, 문장만 AI」 구조이기 때문입니다 (API 명세서 NOW-COACH-001).


CREATE TABLE safety_checks (
    id               text        PRIMARY KEY,
    user_id          text        NULL REFERENCES users(id),      -- 비회원도 호출
    source           text        NOT NULL
        CHECK (source IN ('custom_item', 'custom_signal', 'todo', 'coach', 'care_note', 'plan')),
    matched          boolean     NOT NULL,                       -- 걸렸는지
    matched_keyword  text        NULL,
    text_hash        text        NOT NULL,                       -- ★ 원문을 저장하지 않습니다
    created_at       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_safety_checks_created ON safety_checks (created_at DESC);

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
    id             text        PRIMARY KEY,
    user_id        text        NOT NULL REFERENCES users(id),
    log_date       date        NOT NULL,
    state          text        NULL
        CHECK (state IS NULL OR state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    action_id      text        NULL REFERENCES actions(id),
    done           boolean     NOT NULL DEFAULT false,
    removed_count  smallint    NOT NULL DEFAULT 0,               -- 그날 걷어낸 항목 수
    created_at     timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ux_daily_logs_user_date UNIQUE (user_id, log_date)
);

CREATE INDEX ix_daily_logs_user_date ON daily_logs (user_id, log_date DESC);

-- recordedDays 는 이 테이블의 행 수입니다.
-- 7일이면 주간 리뷰, 30일이면 월간 패턴이 열립니다.
-- ⚠️ daily_logs 유지 여부 미확정 — actions.completed_at 파생안이 제안되어 있습니다.
--    확정 전에는 이 테이블을 쓰는 코드를 늘리지 마십시오.


CREATE TABLE state_transitions (
    id            text        PRIMARY KEY,
    user_id       text        NOT NULL REFERENCES users(id),
    from_state    text        NOT NULL
        CHECK (from_state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    to_state      text        NOT NULL
        CHECK (to_state IN ('energetic', 'normal', 'low', 'drained', 'unknown')),
    signal_score  smallint    NOT NULL,                          -- 제안 근거가 된 점수
    accepted      boolean     NULL,                              -- NULL 이면 아직 응답 없음
    responded_at  timestamptz NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_state_transitions_user_created ON state_transitions (user_id, created_at DESC);

-- 임계값 5 를 넘으면 전환을 제안합니다. 거절하면 유예 기간 동안 다시 묻지 않습니다.
-- ⚠️ 재제안 유예 기간 미확정(예시 3일) — created_at 으로 계산하므로 컬럼은 필요 없습니다.


-- ============================================================================
--  실행 후 확인
-- ============================================================================

-- 22개가 나와야 정상입니다.
-- SELECT count(*) AS 테이블수 FROM information_schema.tables
--  WHERE table_schema = 'public' AND table_type = 'BASE TABLE';

-- 이름 목록으로 빠진 것 확인
-- SELECT table_name FROM information_schema.tables
--  WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
--  ORDER BY table_name;


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
--
--  이 파일과 노션 「DB 설계서」가 어긋나면 노션을 고쳐 주십시오.
-- ============================================================================
