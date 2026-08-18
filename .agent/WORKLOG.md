# WORKLOG — 무엇을 했는가

세션을 끝낼 때 **맨 위에** 추가합니다. 최신이 위입니다.

## 형식

```
## 2026-08-14 15:20~16:40 · claude · today
- 한 것: GET /today · POST /today/reroll
- 쓴 스텁: VerdictPort (subtract 미완이라)
- 남은 것: POST /today/reject 의 none 분기
- 막힌 것: 없음
- 브랜치: feature/today-swonseok → develop 머지 와료
```

**「막힌 것」을 반드시 적으십시오.** 다음 사람이 같은 데서 또 막힙니다.

---

## 2026-08-18 15:00~17:45 · claude · DB (MySQL 전환)

- 한 것: **PostgreSQL → MySQL 8.0.16+ 전환**. 팀이 다룰 수 있는 DB 를 쓰기로 한 결정에 따른 것입니다
  - `schema_v63.sql` 전면 재작성 — 22테이블 · **외래키 28** · **CHECK 23**
  - `build.gradle` 드라이버 교체 · **H2 제거**
  - `application.yml` — `local` 을 MySQL 로, **`prod` 프로파일 신설**
  - DB 이름 **`fromnowon_db`**
- **옮기면서 걸린 것 다섯 — 그대로 옮겼으면 조용히 깨졌을 것들**
  - **인라인 `REFERENCES` 28곳** → 테이블 레벨 `FOREIGN KEY`.
    MySQL 은 컬럼 레벨 `REFERENCES` 를 **문법만 받고 무시**합니다. 오류 없이 외래키만 사라집니다
  - **부분 인덱스 2곳**(1개인 줄 알았으나 `UNIQUE` 가 하나 더 있었습니다). MySQL 에 없는 기능이라 `WHERE` 제거.
    **`ux_users_email` 은 의미가 바뀝니다** — `REQUESTS #7`
  - **`rank`** 는 MySQL 8.0 예약어(윈도 함수) → 백틱
  - **`text`** 컬럼명 2곳 → 백틱
  - `timestamptz`→`DATETIME(6)` · `jsonb`→`JSON` · `text`→`VARCHAR`
- **H2 를 없앤 이유** — 흉내내는 DB 로 검증하면 실서버에서 처음 터집니다.
  이제 `ddl-auto: validate` 가 **엔티티와 실제 테이블을 로컬에서 대조**합니다
- 확인 (**로컬 MySQL 8.0.45 에 실제로 올려서**)
  - 스키마 적용 → **테이블 22 · 외래키 28 · CHECK 23**
  - **`validate` 통과** — `AuthUser` ↔ `users` 일치. 걱정하던 `OffsetDateTime` ↔ `DATETIME(6)` 도 맞았습니다
  - `Started NowApplication in 2.925s` · `POST /auth/guest` **200** ×3
  - SQL 로그에 `insert into users` 확인 · **`users` 3행 실제 확인**
- **막힌 것 · 남은 것**
  - `REQUESTS #6` 배포 서버 MySQL 버전 미확인. **8.0.16 미만이면 `CHECK` 23개가 조용히 무시됩니다**
  - `REQUESTS #7` `users.email` 유니크 의미 변경 — 탈퇴 API 가 생기면 손봐야 합니다
  - **노션 「DB 설계서」가 아직 PostgreSQL 기준**입니다. 사람이 갱신해야 합니다
- 브랜치: `feature/db-mysql-swonseok` **push 만** 완료. `develop` 머지는 사람이
- **참고** — Workbench 가 `autocommit=0` · `REPEATABLE READ` 면 앱이 넣은 행이 안 보입니다.
  트랜잭션 스냅숏에 갇힌 것이라 `ROLLBACK;` 하거나 `Query → Auto-Commit Transactions` 를 켜십시오.
  8/18 에 여기서 30분 헤맸습니다

---

## 2026-08-18 11:50~14:30 · claude · auth · common/id

- 한 것: 작업 트리에 들어와 있던 변경을 **검증하고 넷으로 나눠 커밋**했습니다
  - A `fix` — `excludedBy` 를 `floor` → `medical` (명세서 `NOW-SUB-001` 과 일치)
  - B `refactor` — 사용자 번호 `Long` → `String`. **반쪽만 돼 있던 것을 포트·문서 전체로 확대**
  - C `feat` — 게스트 인증 `POST /auth/guest` · JWT(HS256, 라이브러리 없음) · 토큰 필터
  - D `docs` — DB 스키마 22테이블 · 확인 명령 정정
- 확인: **AuthTokenProviderCheck 17/0** · **SubtractPipelineCheck 31/0** · `Started NowApplication in 2.703s`
  - 게스트 발급 2회 호출 — 토큰·`sub` 가 서로 다름. 헤더가 명세 예시 `eyJhbGciOiJIUzI1NiJ9` 와 일치
  - 필터 등록 확인 — `authTokenFilter urls=[/*] order=1`
- **고친 것 1건**: `AuthUser.createdAt` 에 `@ColumnDefault("now()")`.
  `insertable=false` 만으로는 H2 `create-drop` DDL 에 기본값이 안 생겨
  `POST /auth/guest` 가 **500**(`NULL not allowed for column "CREATED_AT"`)이었습니다.
  실서버 스키마에는 `DEFAULT` 가 있어 안 드러나던 문제입니다
- **막힌 것 · 미검증**
  - `AuthTokenFilter` → 리졸버 **실제 주입 경로는 검증하지 못했습니다.** `@CurrentUser` 를 쓰는
    엔드포인트가 아직 없습니다. **`REQUESTS.md` #5** 에 기록. 「검증됐다」고 착각하지 마십시오
  - **팀이 MySQL 로 가기로 했습니다.** `schema_v63.sql` 은 아직 PostgreSQL 문법입니다.
    다음 브랜치에서 8.0.16+ 로 전환합니다. **`REQUESTS.md` #6**
- 브랜치: `feature/auth-swonseok` **push 만** 완료. `develop` 머지는 사람이
  - **이름과 내용이 일부 어긋납니다** — A·B·D 는 auth 가 아닙니다.
    동결까지 이틀이라 PR 을 늘리지 않는 쪽을 택했고, 커밋으로만 갈랐습니다

---

## 2026-08-17 14:05~14:30 · claude · subtract

- 한 것: 초기 반입분 **첫 빌드 확인**. Gradle 래퍼 생성(8.10.2 고정 유지) · `bootRun` 기동 확인 · 판정 엔진 동작 확인
- **고친 소스 없음.** `compileJava` · `compileTestJava` 가 한 번에 통과했습니다. 버전 3종(Java 21 · Boot 3.3.5 · Gradle 8.10.2)은 손대지 않았습니다
- 래퍼: `gradle` CLI 가 PC 에 없어 8.10.2 배포본을 받아 **빈 임시 디렉터리에서** 생성한 뒤
  `gradlew` · `gradlew.bat` · `gradle-wrapper.jar` 만 복사했습니다.
  `gradle-wrapper.properties` 는 저장소 고정본 그대로 (생성본과 253바이트 동일)
- 확인: **판정 엔진 통과 31 · 실패 0** / `Started NowApplication in 4.779 seconds` (H2 메모리, PostgreSQL 없이)
- 쓴 스텁: `VerdictPortStub` · `NoteRulePortStub`
- 남은 것: `SubtractPipelineCheck` 를 JUnit 으로 옮기기 (지금은 `main` 실행). DB 스키마 나오면 ⑧ 저장 + `VerdictPort` 실구현
- **막힌 것: `./gradlew test` 가 이 PC 에서만 실패합니다.** 한글 경로 + Gradle 워커 argfile 인코딩 문제 —
  `REQUESTS.md #4` 에 원인·확인·선택지 3가지를 적었습니다. **코드 문제가 아닙니다** (영문 경로에서는 `BUILD SUCCESSFUL`)
- 브랜치: `feature/subtract-swonseok` **push 만** 완료. **`develop` 머지는 사람이** (`develop` · `main` 무접촉)
- **이력 재작성 1회 — 처음이자 마지막입니다.** 커밋 접두어를 `[swonseok]` 으로 통일하면서
  이 브랜치의 메시지를 다시 쓰고 `--force-with-lease` 로 밀었습니다. 해시가 전부 바뀌었습니다
  - 확인된 것: 파일 트리는 재작성 전과 **완전히 동일**(diff 0) · `develop` · `main` **무접촉** ·
    push 후 10분 이내였고 PR 도 없어 **남이 받아간 커밋 없음** · 백업 `backup/before-prefix-rewrite` 보존
  - **그 뒤 규칙이 강화되었습니다.** `AGENTS.md` 5장 금지 4번이 이제 `--force-with-lease` 와
    이력 재작성까지 **예외 없이** 금지합니다. **앞으로 이 저장소에서 이력을 되감지 않습니다**
  - 접두어가 틀린 커밋을 발견해도 **고치지 말고 다음 커밋부터 맞추십시오**

---

## 2026-08-14 · 사람(송원석) · 저장소 준비

- 한 것: `AGENTS.md` · `CLAUDE.md` · `docs/` 11개 · `.agent/` 3개 생성
- 코드는 아직 없음. **패키지 폴더도 아직 안 만들었습니다**
- 다음: 이철희 님이 `common/` 과 DB 스키마를 올리면 그때부터 API 착수
- 막힌 것: 인증 방식 미확정 → `auth/` 착수 불가

---

## 2026-08-14 밤 · 사람(송원석) · 요구사항 명세서 전원 점검

- 한 것: 다섯 명 몫을 요구사항 명세서와 대조 → **행이 없던 작업 5건**을 찾아 노션에 신설
  - 스프링 프로젝트 초기 생성(이철희 8/14 14:00) · 공용 기반 `common` 구현(이철희 8/14 16:00)
  - 인터페이스 5개 스텁 커밋(전원 8/14 21:00) · 백엔드 실서버 배포(이철희 8/16 21:00) · 시안 수정 2건(김지현 8/15 12:00)
- 고친 것: 「LLM 호출 2곳」 → **4곳**. 노션 시스템 아키텍처 · `docs/08-open-items.md` 12번
- 비율 정정: 이철희 30.2% → **32.6%** (프로젝트 생성 1h + 배포 2h 가 드러남)
- **에이전트가 알아야 할 것**: 8/14 21:00 이전에는 `docs/04-ports.md` 의 인터페이스가 저장소에 없습니다. **스텁이 올라오기 전에는 남의 패키지를 부르는 코드를 쓰지 마십시오**
- 남은 것: 황인서 「나머지 4화면」 실제 범위 · `excluded` 정의 회신 — **8/15 확인 예정**
