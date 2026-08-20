# REQUESTS — 막힌 것 · 사람에게 넘기는 것

에이전트가 **추측으로 진행하는 대신** 여기에 적습니다.
사람이 답을 채우고 `해결` 로 옮깁니다.

## 열려 있음

| # | 올린이 | 무엇이 막혔나 | 누가 답해야 하나 |
|---|---|---|---|
| 1 | 송원석 | ~~**인증 방식(JWT · 세션 · 매직링크)이 미확정이라 `auth/` 를 시작할 수 없습니다**~~ **2026-08-20 해소 — 8/17 에 확정되고 8/18 에 구현됐습니다.** 게스트 JWT 입니다. `[측]` 실서버 `POST /auth/guest` **200** · 노션 `NOW-AUTH-001` 도 **구현: 완료**로 되어 있습니다. **항목이 열린 채로 남아 있어서 「auth 를 못 시작한다」로 읽힐 수 있었습니다** | 해소 |
| 2 | 송원석 | `daily_logs` 테이블을 만들지, `actions.completed_at` 파생으로 갈지 | 이철희 |
| 3 | 송원석 | ~~**프롬프트 4종 원문이 아직 없습니다. `today` · `item` 의 AI 호출부가 막힙니다**~~ **2026-08-20 해소 — 이미 들어와 있었습니다.** `docs/prompts/` 에 4종이 전부 있고(`01-subtract-reason` 137줄 · `02-today-action` 158줄 · `03-item-interpret` 143줄 · `04-coach-answer` 165줄), 실제 프롬프트 문자열까지 들어 있습니다. 커밋 `bf48e3f`. **각 파일 머리말에 「김민정 님께 / 이철희 님께 — 이 파일의 문자열을 그대로 쓰시면 됩니다」까지 적혀 있습니다.** ⚠️ **이 항목이 열린 채로 남아 있어서 두 분이 「AI 호출부가 막혀 있다」고 읽었을 수 있습니다.** 진짜 미확정은 `docs/prompts/00-index.md:105` 의 셋뿐이고 **프롬프트를 막지 않습니다** — 위기 키워드 목록(김민정 · `safety/` 용) · `rerollLeft` 한도(이철희 · 초안 3) · 하루 코치 호출 한도(초안 20) | 해소 |
| 4 | claude | **경로에 한글이 있는 Windows 에서 `./gradlew test` 가 실패합니다.** 코드 문제가 아니라 빌드 환경 문제라 판단이 필요합니다 (아래 상세) | 이철희 (`build.gradle` 소유) |
| 5 | claude | ~~**`AuthTokenFilter` → `CurrentUserArgumentResolver` 경로 미검증.** `@CurrentUser` 를 쓰는 엔드포인트가 하나도 없어서입니다~~ **2026-08-20 해소 — 실서버에서 증명했습니다.** `@CurrentUser` 를 쓰는 컨트롤러가 둘 생겼습니다(`CheckinController` · `SubtractController`). `[측]` 게스트 토큰 **두 개**를 따로 받아 A 로 체크인을 만들고 조회하니 **A=200 · B=409** 였습니다. **남의 것이 안 보인다는 것이 곧 리졸버가 토큰마다 다른 값을 실제로 꺼낸다는 뜻**입니다. 필터 등록만 확인하고 「검증됐다고 착각하지 마십시오」라고 적어 뒀던 항목입니다 | 해소 |
| 6 | claude | ~~**배포 서버의 MySQL 버전 미확인.** `CHECK` 23개가 8.0.15 이하에서는 조용히 무시됩니다~~ **2026-08-20 해소.** `[측]` 실서버 `SELECT VERSION()` → **8.0.46**. 8.0.16 이상이라 `CHECK` 가 실제로 걸립니다. 같이 센 것 — **테이블 22 · 외래키 28 · CHECK 23**. 파일 끝 확인 쿼리의 기대값과 전부 일치합니다 | 해소 |
| 7 | claude | **`users.email` 유니크 조건의 의미가 바뀌었습니다.** 원본은 `UNIQUE (email) WHERE deleted_at IS NULL`(살아 있는 회원끼리만 유일)인데 **MySQL 에 부분 유니크 인덱스가 없어** 그냥 `UNIQUE (email)` 로 갔습니다. **탈퇴한 회원의 이메일도 계속 막습니다.** 지금은 탈퇴 API 가 없어 드러나지 않지만, 생기면 탈퇴 시 `email` 을 비우거나 별도 컬럼으로 옮겨야 합니다. **제가 정할 사안이 아니라 그대로 두고 올립니다** | 송원석 |
| 8 | claude | **H2 를 제거하고 로컬도 MySQL 을 쓰도록 바꿨습니다(8/18).** 이제 `ddl-auto: validate` 가 엔티티와 실제 테이블을 대조하므로 **스키마 불일치가 서버가 아니라 로컬에서 걸립니다.** 대신 **각자 PC 에 MySQL 8.0.16+ 와 `fromnowon_db` 가 있어야 `bootRun` 이 됩니다.** 절차는 `application-local.yml.example` 머리말 참고 | 전원 (각자 로컬) |
| 9 | claude | **`reason` 을 `VARCHAR(500)` 으로 잡았습니다. 명세에 없는 값이라 제가 지어낸 것입니다.** 8/19 회의 안건 3의 **기본값은 `TEXT` 전환**이었으나 **회의 결과를 받지 못했습니다.** 지금은 `VARCHAR(500)` 그대로입니다 | 송원석 (회의 결과 확인) |
| 10 | claude | **환경 변수 이름 5개(`DB_URL` · `DB_USER` · `DB_PASSWORD` · `CORS_ORIGINS` · `APP_JWT_SECRET`)를 제가 정했습니다.** 8/19 회의 안건 4의 **기본값은 현행 유지**였으나 **회의 결과 미확인**입니다. 이철희 님이 다른 이름으로 배포 스크립트를 준비 중이면 어긋납니다 | 이철희 |
| 11 | claude | **`safety_checks.source` 에서 `todo` 를 제거했습니다(8/20 적용 완료).** `NOW-SAFE-001` 의 사전 필터 경로가 다섯 곳이고 코드 열거형 5개와 1:1 이며, 「오늘의 행동」에 자유 입력이 없어 `todo` 에 대응하는 화면도 API 도 없습니다. **정한 사람** 코워크 · **언제** 2026-08-19 · **근거** `DISCREPANCIES-0819` E-5. **`docs/schema_v63.sql` 은 예외 승인(E-6)으로 직접 고쳤습니다** | 해결 대기 없음 (기록) |
| 12 | claude | **`actions.rank` 는 MySQL 8.0 예약어(윈도 함수)입니다.** 엔티티에서 백틱으로 감싸지 않으면 **앱이 뜨지 않습니다** — ``@Column(name = "`rank`")`` 처럼 컬럼명을 백틱으로 감싸십시오. **`care_note_lines.text` · `coach_messages.text` 도 같습니다.** `actions` 는 김민정 님 소유입니다 | 김민정 (엔티티 작성 시) |
| 13 | claude | ~~**JWT 무효화 수단이 없습니다.** 로그아웃해도 토큰이 만료까지 삽니다~~ **2026-08-20 정정 — 결함이 아니라 명세서가 정한 설계입니다.** 노션 `NOW-AUTH-004` 비고가 **「서버 stateless」**이고 본문에 **「서버는 stateless입니다. 클라이언트가 토큰을 삭제합니다. 재발급 방지가 필요하면 블랙리스트 처리를 검토합니다」**로 적혀 있습니다. `[측]` `sessions` 테이블을 코드가 쓰지 않는 것도 그 설계와 **일관**됩니다 — 빈 것이 정상입니다. **진짜로 열려 있는 것은 만료 기간 하나뿐입니다** — 현재 30일(`valid-seconds: 2592000`)이고 8/19 회의 안건 7의 기본값이 「7일로 단축」이었는데 **회의 결과를 아직 못 받았습니다.** `[문]` 노션에는 기간 수치가 없습니다(필드 `expiresAt` 만 정의) | 송원석 (회의 결과 확인) |
| 14 | claude | **`care_items.core` · `base` 를 `SMALLINT` → `DECIMAL(3,1)` 로 바꿨습니다(8/20 적용 완료).** 마스터 시드에 **소수가 9건**(`core 2.5` 2건 · `base 0.8 1.2 1.4 1.6 2.2 2.5 3.5` 각 1건) 있어 반올림되면 `load` 가 어긋나고 임계값 `0.8`·`-2.5` 에서 **판정이 뒤집힙니다.** **찾은 사람** 코워크 · **언제** 2026-08-19 · **근거** `DISCREPANCIES-0819` N-1. **예외 승인(E-6 확대)으로 `docs/` 를 직접 고쳤습니다** | 해결 대기 없음 (기록) |
| 15 | claude | **`ErrorCode` 열거형과 명세서 공통 에러 코드 목록이 다릅니다.** 코드 **17종** · 노션 공통 표 **9종** · 겹치는 것 **6종**. 노션 정리는 **코워크**가 합니다. **`ITEM_NOT_SELECTED` · `ITEM_EXCLUDED` 는 공통 표에만 있고 어느 API 실패 코드 표에도 없어 코드에 넣지 않았습니다** — 서버 측 검증 1·8번은 오류가 아니라 「제거」로 처리하기 때문입니다. **근거** `DISCREPANCIES-0819` N-6 · 회신 7 · 3장 | 코워크 (노션) |
| 16 | claude | **`CANNOT_REVERT_EXCLUDED` 가 명세서 어디에도 없습니다.** `NOW-SUB-003` 실패 코드 표에도 없습니다. **이 앱의 차별점(클리닉 안내는 앱이 판단하지 않으므로 되돌리기가 없다)이 코드에만 있습니다.** 노션 갱신은 코워크가 합니다 · **근거** `DISCREPANCIES-0819` 회신 7 · 3장 | 코워크 (노션) |
| 17 | claude | **팀 규칙 문서(`SWONSEOK-RULES-0817.md` · `HANDOFF.md` · `CLAUDE-CODE-HANDOFF-0817.md`)가 저장소 밖에 있어 버전 관리가 안 됩니다.** PR 에도 안 들어가고 이력도 남지 않습니다. **제출 후 정리 대상** | 송원석 (제출 후) |
| 18 | claude | ~~**OpenAI 크레딧을 쓰기로 했습니다(8/20).** `LlmClient` 구현체가 아직 없습니다 — 인터페이스만 있고 **`common/llm/` 은 송원석 소유입니다**~~ **2026-08-20 15:55 정정 — 소유자를 제가 잘못 적었습니다.** `[문]` `docs/03-packages.md:7` 이 **`common/` 을 이철희 님**으로 두고 「LLM 클라이언트」를 그 안에 명시합니다. `[문]` `docs/02-roles.md:17` 도 이철희 님입니다. **제가 확인 없이 「송원석 소유」라고 썼고 그것을 근거로 삼았습니다.** **구현 자체는 끝났습니다** — `8f04b51`·`d54ebd8`, PR #9 로 `develop` 에 있습니다. 모델 `gpt-5.6-luna` · 환경 변수 `FROMNOWON_OPENAI_API_KEY`·`FROMNOWON_OPENAI_MODEL` · 의존성 추가 없음 | 해소 (소유 정정은 #42) |
| 19 | claude | ~~**`ItemPort` · `CheckinPort` 가 `docs/04-ports.md` 에 선언만 있고 파일이 없습니다**~~ **2026-08-20 해소.** 둘 다 있습니다 — `item/ItemPort.java` · `checkin/CheckinPort.java`. `CheckinPort` 는 `CheckinPortAdapter` 로 실제 구현까지 붙었고, `ItemPort` 는 스텁이 빈 목록을 돌려줍니다(의도). `ItemPort` 시그니처는 8/20 에 넓혔습니다 — **`REQUESTS #30`** 참고 | 해소 |
| 20 | claude | **API 명세서의 담당란이 낡았습니다.** `NOW-TODAY-*` · `NOW-HOME-*` · `NOW-LOG-*` · `NOW-NOTE-*` 가 **이철희**로 적혀 있는데, `docs/03-packages.md` 기준으로 **`today/` · `home/` · `log/` · `care/` 는 김민정 님 패키지**입니다. **저장소의 `03-packages.md` 가 정본**입니다. 노션 명세서 담당란을 그쪽에 맞춰야 합니다. **8/20 김민정 님 질문에 답하다 발견했습니다** | 코워크 (노션) |
| 21 | claude | **2026-08-20 해소 — `fk_care_note_rules_line` 에 `ON DELETE CASCADE` 를 걸었습니다 (`db/patch_note_tables_v1.sql`).** 김민정 님 `PUT /me/care` 가 「통째로 갈아 끼우기」라 여기서 실제로 막혔습니다 — `[측]` `DELETE FROM care_notes` → `ERROR 1451`. 지금은 `lines` · `rules` 가 같이 지워집니다. 아래는 원문입니다. **`care_note_rules` 를 지울 때 순서가 강제됩니다.** 안내문 계통 외래키가 둘인데 하나만 `CASCADE` 입니다 — `fk_care_note_rules_note` 는 `ON DELETE CASCADE` 이고 **`fk_care_note_rules_line` 에는 없습니다.** 그래서 `care_notes` 를 먼저 지우면 `care_note_lines` 로 번지는데 **그 줄을 `rules` 가 붙잡아 실패합니다.** 지울 때는 **`rules` → `notes` 순서**로 해야 합니다. 스키마를 고치려면 `fk_care_note_rules_line` 에 `ON DELETE CASCADE` 를 붙이면 되는데, **`docs/` 예외를 또 여는 것이라 올려만 둡니다.** 8/20 실측 | 해소 (기록) |
| 22 | claude | **LLM 호출 규약을 실측에 맞춰 고쳤습니다(8/20).** 타임아웃 **6초 → 10초** · 재시도 **1회 → 0회** · **`reasoning_effort: low`** 신설. 근거는 팀 OpenAI 키로 `gpt-5.6-luna` 를 9회 호출한 실측입니다 — 세 추론 단계 모두 6초를 넘었고(평균 6.06~6.45초), **시간을 먹는 것은 추론이 아니라 출력량**이었습니다(`none` 은 추론 토큰 0개인데도 6.06초). 재시도를 뺀 이유는 타임아웃만 늘리면 최악이 20초가 되기 때문이고, `response_format` 으로 JSON 이 강제되어 형식 실패가 막혀 있습니다. **`docs/prompts/00-index.md` 는 예외 승인으로 직접 고쳤습니다** — 근거 `DISCREPANCIES-0819` 회신 15 · 6장. ⚠️ **항목 15개 초과와 실서버 네트워크에서는 재지 않았습니다.** `LlmClient` 를 붙인 뒤 다시 재야 합니다 | 코워크 승낙 완료 (기록) |
| 23 | claude | **2026-08-20 종결 — 바꾸지 않기로 했습니다(송원석 결정).** 저장소가 비공개이고 제출 후 서버를 내립니다. 아래는 기록입니다. **MySQL `nowapp` 비밀번호가 `1234` 이고 계정명이 커밋에 있습니다.** `db/seed_carenote_demo.sql:36` 의 실행 예시에 `-u nowapp` 이 들어 있습니다. **지금은 위험하지 않습니다** — 3306 이 밖에서 닫혀 있는 것을 실측했습니다. **다만 대회 규정이 「저장소 공개 필수」라 제출 시 공개로 바뀝니다.** 그러면 계정명 노출 + 약한 비밀번호 + (누가 3306 을 열면) 즉시 침해입니다. **제출 직전(8/21) 또는 공개 전환 전에 교체하십시오.** `/etc/fromnowon.env` 의 `DB_PASSWORD` 도 같이 바꾸고 재시작해야 합니다. **지금 안 하는 이유는 재시작이 필요하고 남이 서버를 쓰고 있어서**입니다 | 해소 (기록) |
| 24 | claude | ~~**`evaluation_results.evidence_level` 을 `medium` 으로 고정해 두었습니다**~~ **2026-08-20 해소.** `ItemPort` 를 넓혀 근거 등급이 실제로 흘러옵니다(`REQUESTS #30`). `SubtractService.evidenceCode()` 가 숫자를 `high`/`medium`/`low`/`none` 으로 옮깁니다. **하드코딩은 없어졌습니다** | 해소 |
| 25 | claude | **`checkin/` 이 `subtract/SubtractCondition` 을 씁니다 — 패키지 순환입니다.** 상태 5종과 `judgeStrength` 매핑을 두 벌로 만들지 않으려고 그렇게 했습니다. `subtract/SubtractService` → `checkin/CheckinPort` 이므로 패키지끼리 서로를 참조합니다. **클래스 순환이 아니라 동작에는 문제가 없고 둘 다 송원석 소유**입니다. **동결 후 `common/` 으로 옮기는 것을 권합니다** — 상태 5종은 `checkin` · `subtract` · `today` · `home` 이 함께 쓰는 공통 어휘입니다 | 송원석 (제출 후) |
| 26 | claude | ~~**첫 발자국 8건의 `is_onboarding` 을 정할 근거가 어디에도 없습니다.**~~ **2026-08-20 철회 — 제가 틀렸습니다.** 노션 `NOW-STEP-001` 응답 예시에 **`"onboardingIds": ["fs_101", "fs_104", "fs_107", "fs_108"]`** 가 그대로 있습니다. `footsteps.ts` 순서로 **p1(수면) · p4(마음) · p7(운동) · p8(일상)** 이고 카테고리가 전부 다릅니다. **제가 `docs/` 와 프로토타입만 보고 노션 명세서를 안 봤습니다.** 자료 우선순위(노션 > `docs/` > 프로토타입)를 알면서 건너뛴 것입니다. 재발 방지는 `CC-RULES-0820.md` `CC-1`·`CC-2` | 해소 |
| 27 | claude | ~~**첫 발자국 id 형식이 문서 세 곳에서 서로 다릅니다.**~~ **2026-08-20 철회 — 답이 있었습니다.** 노션 `NOW-STEP-001` 이 **`fs_101`** 로 정하고 있습니다 (`p1`=`fs_101` … `p8`=`fs_108`). 제가 든 세 곳은 `docs/`(4순위)와 프로토타입(5순위)뿐이었고 **3순위인 노션을 안 봤습니다.** ⚠️ **다만 진짜 어긋남이 하나 남습니다** — `docs/07-response-rules.md:58` 이 `"footstepId": "fs3"` 인데 노션은 `fs_101` 입니다. **우선순위상 노션이 맞고 `docs/` 를 고쳐야 합니다.** `docs/` 는 제가 안 고칩니다 | 코워크 (`docs/` 정정) |
| 28 | claude | **`categories` 에 아이콘을 넣을 칸이 없습니다.** 원본 `CATS` 는 `ic:'💧'` 같은 이모지를 함께 갖고 있는데 `schema_v63.sql:125` 의 `categories` 는 `id·name·sort_order` 뿐입니다. **화면 것이라 서버가 안 갖는 편이 맞다고 보고 시드에서 뺐습니다.** 프론트가 `src/data/items.ts` 의 `CATS` 를 그대로 쓰면 됩니다. **아이콘을 서버에서 내려야 한다면 컬럼 추가가 필요**하니 그때 말씀해 주십시오 | 황인서 (프론트) · 코워크 |
| 29 | claude | **2026-08-20 해소 — 네 건은 명세값으로, 26건은 공식 유지(김민정 님과 합의 · 커밋 `f6e2c67`).** 아래는 기록입니다. **관리 항목 32건 중 26건의 `minutes` 가 마스터 데이터에 없어, 원본의 계산식을 그대로 옮겼습니다.** `jigeumbuteo_app_v6.3.html:1212` 에 `T=[4,5,7,10,12,15,18]` · `mn = T[(id 문자코드 합) % 7]` 이 있고, 원본 주석이 **「v5는 매 호출마다 무작위였습니다」**라고 적어 **결정적으로 바꾼 것이 의도**임을 밝히고 있습니다. **지어낸 값이 아니라 원본 계산 결과**입니다. 다만 **id 문자열이 바뀌면 값도 바뀝니다** — 항목 id 를 `cr1` 에서 다른 형식으로 옮기면 화면의 「N분」이 통째로 달라집니다. **그래서 id 를 원본 그대로 뒀습니다.** 참고로 `sl1`(7시간 이상 자기)이 18분인 것은 **수면 시간이 아니라 그 항목의 「오늘 할 행동」 소요 시간**입니다 | 해소 (기록) |
| 30 | claude | **`ItemPort.SelectedItem` 을 넓혔습니다(8/20). `docs/04-ports.md:56` 과 달라졌습니다.** 넣은 것은 `userItemId` · `name` · `categoryId` · `core` · `base` 다섯입니다. **왜 — ① 점수식이 `core` 와 `base` 를 쓰는데 창구가 안 날라서 `SubtractService` 가 0 을 박고 있었습니다.** 스텁이 빈 목록이라 안 드러났을 뿐, 실제 구현이 들어오면 **모든 항목 점수가 같아져 판정이 전부 동일**해집니다. **② `evaluation_results.user_item_id` 의 외래키가 `user_items(id)` 인데 마스터 ID(`cr4`)를 넣고 있었습니다** — 저장이 터질 상태였습니다. **③ 명세서가 `results[].name` 을 필수로 두는데 `itemId` 를 대신 넣고 있었습니다.** `docs/` 는 안 고쳤습니다. **채우는 쪽은 이철희 님** — `user_items × care_items` 조인 한 번이고 마스터 시드는 들어갔습니다 | 이철희 · 코워크 (이견 확인) |
| 31 | claude | **`GET /subtract/latest` 를 `GET /subtract/result` 로 바꿨습니다(8/20).** 노션 명세서 `NOW-SUB-002` 와 `docs/05-api-list.md:51` 이 둘 다 `/result` 인데 서버만 `/latest` 였습니다. **프론트 목 함수도 `/subtract/result` 를 부릅니다** — 그대로 뒀으면 스위치를 켜는 순간 404 였습니다. 같이 고친 것 — 응답 배열 `items`→`results`, `createdAt`·`filter` 신설, `?verdict=`·`?evaluationId=` 지원, 되돌리기 응답을 `{itemId, verdict, persisted, summary}` 로, 되돌리기 요청 본문 `{evaluationId}` 필수 | 기록 (조치 완료) |
| 32 | claude | **2026-08-20 해소 — 노션 `NOW-SUB-001` 실패 코드 표에 「3개 이상」이 있어 3 으로 확정.** 아래는 기록입니다. **`POST /subtract/evaluate` 가 항목 3개 미만일 때 200 을 내보내고 있었습니다(8/20 수정).** 명세서 `NOW-SUB-001` 은 400 `MIN_ITEMS_REQUIRED` 이고, 같은 문서에 **「어떤 경우에도 빈 결과를 반환하지 않습니다」**가 있는데 정반대였습니다. 오류 문구도 명세서 문장(「관리 항목을 3개 이상 선택해 주세요」)으로 맞췄습니다. ⚠️ **최소 개수 3은 `schema_v63.sql:232` 이 「미확정(제안값 3)」으로 적어 둔 값**입니다. 확정되면 `SubtractService.MIN_ITEMS` 한 줄입니다 | 해소 (기록) |
| 33 | claude | **2026-08-20 해소 — `jdbc.time_zone` + `timezone.default_storage: NORMALIZE` 적용, 실서버 `+09:00` 확인.** 아래는 기록입니다. **시각이 9시간 어긋나고 있었습니다(8/20 수정).** `DATETIME(6)` 에는 시간대가 없고 MySQL 이 `CURRENT_TIMESTAMP(6)` 로 KST 를 적는데, 하이버네이트가 그것을 UTC 로 읽어 `09:49 KST` 가 `09:49Z`(=18:49 KST)로 나갔습니다. **화면의 「방금」이 9시간 뒤 미래로 보입니다.** `hibernate.jdbc.time_zone: Asia/Seoul` + `timezone.default_storage: NORMALIZE` 로 고쳤습니다 (앞의 것만으로는 안 됩니다 — 하이버네이트 6이 `OffsetDateTime` 을 기본으로 UTC 정규화하면서 무시합니다). ⚠️ **앱이 직접 쓰는 유일한 시각이 `users.last_login_at`** 인데, 이 변경 전에 쓰인 행은 UTC 이고 이후는 KST 라 **9시간 어긋난 채 섞입니다.** 읽는 코드가 없어 그대로 뒀습니다 | 해소 (기록) |
| 34 | claude | ~~**`SignalWeightPortStub` 이 여전히 빈 지도를 돌려줍니다.**~~ **2026-08-20 해소** — `master/SignalWeightAdapter` 로 교체했고 스텁은 지웠습니다. 실측 — `sig_01`(2) + `sig_04`(3) = 5 → 임계 도달 → 전환 제안 뜹니다. 없는 번호는 무시되고 예외를 던지지 않습니다. **PM 승인 2026-08-20.** ⚠️ **`master/` 에 파일 셋이 들어갔습니다**(`MasterSignal` · `MasterSignalRepository` · `SignalWeightAdapter`). 마스터 API 3건은 손대지 않았습니다. **김민정 님이 같은 일을 하는 `@Component` 를 만드시면 빈이 둘이라 앱이 안 뜹니다** — 그때 이 셋을 지우십시오. 원래 적었던 것 — 그래서 마스터 징후를 몇 개 고르든 `signalScore` 가 **0** 이고, 임계값 5 를 못 넘어 **상태 전환 제안이 한 번도 일어나지 않습니다.** 스텁 주석에 「시드가 들어오면 그대로 값이 흐릅니다」로 적어 뒀는데 **시드는 2026-08-20 에 들어갔습니다.** 실제 구현은 `signals` 를 읽는 것이고 **`master/` 는 김민정 님 폴더**라 제가 만들지 않았습니다. **직접 입력 징후는 각 2점이라 그쪽으로는 점수가 오릅니다** | 김민정 · 코워크 |
| 35 | claude | **`POST /auth/guest` 에 호출 한도가 없습니다.** `[문]` 노션 `NOW-AUTH-001` 실패 코드 표에 **429 `RATE_LIMITED`「호출 한도를 초과했습니다」**가 있는데 `[측]` 코드에 구현이 **0건**입니다 (`ErrorCode.RATE_LIMITED` 선언만 있고 던지는 곳이 없습니다). **인증이 필요 없는 API 라 누구나 무한히 부를 수 있고, 부를 때마다 `users` 행이 하나씩 생깁니다.** 지금은 비공개 저장소·데모라 위험이 낮지만 **제출 때 저장소가 공개로 바뀌면 서버 주소도 같이 알려집니다.** 같은 자리에 코치 호출 한도(초안 20)도 미구현입니다. `auth/` 는 이철희 님 소유라 제가 안 만들었습니다 | 이철희 · 송원석 (제출 전 판단) |
| 36 | claude | **`@CurrentUser` 를 안 쓰는 엔드포인트는 인증이 아예 안 걸립니다.** `[측]` `AuthTokenFilter` 는 **거부를 하지 않습니다** — 토큰이 있으면 요청 속성에 넣고 없으면 그냥 넘깁니다. 401 은 `CurrentUserArgumentResolver` 가 속성을 못 찾을 때만 납니다. **즉 인증은 필터가 아니라 「그 파라미터를 받았는가」로 갈립니다.** `[측]` 김민정 님 PR #8 의 `GET /footsteps` 가 **토큰 없이 200** 입니다. `[문]` 노션 `NOW-STEP-001` 은 `Authorization` 필수 · 실패 코드에 401 `UNAUTHORIZED` 를 두고 있습니다. **개인 데이터가 아니라 지금 새는 것은 없지만, 마스터 API 3건도 같은 상태가 됩니다.** 세 갈래입니다 — ⓐ 각 컨트롤러가 `@CurrentUser` 를 받는다(안 써도 401 은 걸립니다) · ⓑ 필터에서 화이트리스트(`/auth/guest` 만 열기)로 전역 차단 · ⓒ 명세서의 401 을 지운다. **ⓑ 는 `common/` 이라 이철희 님 소유이고 모든 API 에 영향이 갑니다.** 제가 정하지 않습니다 | 이철희 · 코워크 |
| 37 | claude | **`VerdictPort` 를 넓혔습니다(8/20). `docs/04-ports.md:44` 와 달라졌습니다.** `VerdictSet` 에 **`evaluationId`**, `ItemVerdict` 에 **`userItemId`** 를 더했습니다. **왜 — `[문]` `schema_v63.sql:344` 의 `actions` 가 `evaluation_id` 와 `user_item_id` 를 둘 다 `NOT NULL` 외래키로 요구하는데 창구가 그 둘을 안 날랐습니다.** 김민정 님이 `today/` 를 만들어도 **`actions` 행을 만들 수가 없습니다.** `[측]` 소비자가 아직 없어(`Summary` 만 쓰이고 있었습니다) 지금 고치는 것이 가장 쌉니다. 같이 한 것 — **`VerdictPortStub` 을 지우고 `VerdictPortAdapter` 로 교체**했습니다. 스텁이 `Optional.empty()` 와 `Summary(0,0,0,0,0)` 을 돌려줘 **`today/`·`home/` 이 붙어도 화면이 조용히 비었습니다.** ⚠️ **`itemId`(마스터 ID)는 `ItemPort` 를 거쳐 나오는데 그쪽이 아직 스텁이라, 지금은 `userItemId` 가 그대로 옵니다.** `actions` 를 만드는 데 필요한 `userItemId`·`evaluationId` 는 지금도 정확합니다 | 김민정 · 코워크 (이견 확인) |
| 38 | claude | **명세가 「모른다」일 때의 `level` 을 정하지 않았습니다.** `[문]` 노션 `NOW-COACH-001` 은 `level` 을 **`no` · `soft` · `ok` 셋**으로 두는데, 같은 EPIC 의 `docs/prompts/04-coach-answer.md` 는 **「안내문에 없는 항목」·「의학적 판단」·「제품 추천」 세 경우에 「모른다」로 답하라**고 합니다. **모른다는 것은 셋 중 어느 것도 아닙니다.** `ok` 를 넣으면 「해도 된다」로 읽히고 `no` 를 넣으면 하지 않은 판단을 한 것이 됩니다. **그래서 그 경우에는 `level` 을 비웠습니다**(`NON_NULL` 이라 필드 자체가 빠집니다). 화면이 `level` 없는 응답을 못 다루면 알려 주십시오 | 황인서 · 코워크 |
| 39 | claude | **브랜드명·제품명 목록이 없습니다.** `[문]` 노션 `NOW-COACH-001` 이 **「응답에 브랜드명·제품명이 감지되면 `TEXT_REJECTED`(400)로 막고 재생성한다」**고 하는데 **무엇이 브랜드인지 목록이 없습니다.** `[측]` 저장소·프로토타입·시안 어디에도 없습니다. **지어내지 않고 의학적 단정만 걸러 냅니다** (「치료됩니다」「낫습니다」「효과가 있습니다」「에 좋습니다」 등 9개 · `CoachSentences.BANNED`). 같이 거르는 것 — 120자 초과 · 느낌표. **목록을 주시면 그 상수 하나만 바뀝니다** | 김민정 (기획) |
| 40 | claude | **코치 응답 재생성을 안 넣었습니다.** `[문]` 명세는 금칙어가 걸리면 **1회 재생성** 후 그래도 걸리면 사전 문장인데, **바로 사전 문장으로 갑니다.** `[문]` 같은 팀 규약(`docs/prompts/00-index.md`)이 **「재시도 없음 · 최악 대기 10초 고정」**이고, 재생성은 곧 두 번째 호출이라 **최악 대기가 20초**가 됩니다. `[측]` 8/20 실측에서 한 번 호출이 평균 6.4초였습니다. **둘이 충돌해서 규약 쪽을 따랐습니다.** 재생성을 넣으려면 타임아웃 규약을 같이 고쳐야 합니다 | 코워크 |
| 41 | claude | ~~**뷰티 문장이 위기 신호로 잘못 잡힙니다.**~~ **2026-08-20 16:20 결정 — 「사라지고 싶」·「없어지고 싶」 두 갈래를 뺐습니다.** 공백 있는 형태까지 네 줄이 빠져 **24개 → 20개**. 김민정 님 제안 · 송원석 님 결정. **왜** — 뷰티 앱이라 「여드름 자국 없어지고 싶어요」·「기미 사라지고 싶다」가 실제로 들어오고, 피부 고민에 자살예방 상담번호가 뜨는 것도 좋지 않습니다. ⚠️ **대가** — `[측]` 「그냥 사라지고 싶어요」·「나 없어지고 싶어요」도 이제 **안 걸립니다.** 그러면 코치가 **「받으신 안내문에는 그 내용이 없습니다」**로 답합니다. `[측]` 키워드에 없는 「없어져 버리고 싶어요」가 실서버에서 지금 그렇게 답하고 있습니다. **되돌리려면 네 줄을 다시 넣으면 됩니다.** 더 좁은 방법(키워드는 두고 **바로 앞 네 글자에 피부 명사**가 붙었을 때만 제외)을 제안했으나 **채택되지 않았습니다** — 필요하면 20분입니다 | 결정 완료 (기록) |
| 42 | claude | **`common/` 에 파일 셋을 넣으면서 슬랙 공지를 빠뜨렸습니다.** `[문]` `docs/02-roles.md:25` — **「`common/` 을 고치실 분은 슬랙 `backend` 에 먼저 올려 주십시오」**. `[측]` 제가 넣은 것 — `common/llm/OpenAiLlmClient` · `LlmProperties` · `LlmConfig` (`8f04b51`). **올리지 않았습니다.** 근거로 삼은 `REQUESTS #18` 의 「송원석 소유」가 제가 확인 없이 쓴 문장이었습니다(#18 정정). ⚠️ **`common/` 은 셋이 쓰는 유일한 공유 지점**이라 한 사람이어야 한다고 `docs/02-roles.md:36` 이 적고 있습니다. **되돌릴지, 그대로 두고 소유를 정리할지 이철희 님이 정하십니다.** 기존 파일은 안 건드렸고 **새 파일 셋만 넣었습니다** — 지우면 그대로 원상복구됩니다 | 이철희 · 코워크 |
| 43 | FeHee | ~~**`NOW-ITEM-002`의 요청·응답 JSON 계약이 저장소에 없습니다.**~~ **2026-08-20 해소.** 노션 명세 확인 — 요청은 `{ "items": [{ "itemId", "frequency" }] }`, 3개 이상 일괄 저장이고 성공은 `{ "saved", "needsRejudge": true }`입니다. | 해소 |
| 44 | FeHee | ~~**`NOW-ITEM-002`가 검증해야 할 `care_items` 모델이 아직 `develop`에 없습니다.**~~ **2026-08-20 해소.** `item/`에 마스터 엔티티를 복제하거나 다른 패키지 Repository를 호출하지 않고, `ItemUserItemRepository`의 읽기 전용 네이티브 조회로 항목 존재·빈도 가능 여부를 검증하고 `ItemPort` 조인도 처리했습니다. `./gradlew.bat compileJava` 성공. 마스터 PR이 머지돼도 엔티티·빈 중복이 생기지 않습니다. | 해소 |
| 45 | FeHee | **2026-08-20 해소 — `user_items` 에 `custom_category` · `interpreted_by` 두 칸을 넣었습니다 (`db/patch_user_items_custom_v1.sql` · PR #18).** 근거는 `NOW-ITEM-001` 응답 예시가 직접 입력 항목에도 `category` 를 돌려주기 때문입니다 — 저장하지 않으면 만들 때 `move`, 조회하면 `life` 가 됩니다. `evidenceLevel` 은 `interpreted_by` 에서 나옵니다(`llm`→`low` · `fallback`→`none`). 아래는 원문입니다. **직접 입력 항목의 카테고리를 영속할 위치가 없습니다.** `user_items`에는 `custom_name`·`frequency`만 있고, `NOW-ITEM-003`이 LLM으로 추출하는 `category`·`evidenceLevel`·`interpretedBy` 컬럼은 없습니다. 지금 `ItemPortAdapter` 조회의 `coalesce(ci.category_id, 'life')`는 직접 입력을 항상 life로 읽게 되어 실제 LLM 분류값을 잃습니다. `POST /me/items/custom`과 `GET /me/items`를 명세대로 맞추려면 컬럼 추가 또는 별도 테이블의 결정이 필요합니다. | 해소 (기록) |
| 46 | claude | **`NOW-ITEM-003` 의 실패 코드 `503 LLM_UNAVAILABLE` 이 같은 문서의 폴백 동작과 어긋납니다.** 폴백 응답 예시는 `ok: true` 에 `data` 가 있는 **성공 응답**이고, 폴백 동작 표는 「LLM 호출 실패 → 기본값 적용하고 `interpretedBy` 를 `fallback` 으로」입니다. 그런데 실패 코드 표에 `503` 이 있고 문구가 **「해석에 실패해 기본값으로 등록했습니다」** — **등록은 됐다는 뜻**이라 503 과 맞지 않습니다. `NOW-SUB-001` 도 같은 구조이고, 저는 거기서 `200` + `generatedBy: "fallback"` 으로 구현했습니다. `LLM_UNAVAILABLE` 은 `ErrorCode` 에 있지만 **어디에서도 던지지 않습니다**. 이철희 님께 **「201 로 만들고 503 은 쓰지 마십시오」**로 답했습니다. 명세를 그렇게 정리해 주시면 좋겠습니다 | 코워크 (노션) |
| 47 | claude | **`docs/schema_v63.sql` 에 `user_items` 컬럼 2개가 빠져 있습니다.** `NOW-ITEM-003` 이 `custom_category` 와 `interpreted_by` 를 요구합니다 — `NOW-ITEM-001` 응답 예시가 직접 입력 항목에도 `category` 를 돌려주기 때문입니다(저장 안 하면 만들 때 `move`, 조회하면 `life`). `db/patch_user_items_custom_v1.sql` 로 적용해 뒀고 CHECK 둘(`ck_user_items_custom` · `ck_user_items_interpreted`)도 같이 넣었습니다. **`docs/` 는 규칙상 제가 못 고칩니다.** 원본에 반영해 주십시오 | 코워크 (`docs/` 정정) |
| 48 | claude | **회원 인증 방식이 `이메일 + 비밀번호` 로 확정됐습니다(2026-08-20 · 송원석 결정).** `NOW-AUTH-002` 요청에 `password` 필수(8~64자), `NOW-AUTH-003` 요청에도 `password` 필수. **로그인 실패는 `404 USER_NOT_FOUND` 가 아니라 `401 INVALID_CREDENTIALS` 입니다** — 「이메일이 없다」와 「비밀번호가 틀리다」를 구분하면 가입된 이메일 목록을 만들 수 있습니다(계정 열거). `ErrorCode` 에 `EMAIL_ALREADY_EXISTS` · `INVALID_CREDENTIALS` · `USER_NOT_FOUND` 셋을 더합니다. **스키마 변경 없습니다** — `users.password_hash VARCHAR(255)` 가 이미 있고 BCrypt 해시는 60자입니다. `build.gradle` 에 `org.springframework.security:spring-security-crypto` 한 줄 (`[측]` 전이 의존성 0개 · Boot 3.3.5 가 6.3.4 로 관리 · 버전 안 바뀜). **노션 `NOW-AUTH-002`·`003` 본문을 이 내용으로 고쳐 주십시오** — 지금은 `password` 가 없고 `NOW-AUTH-003` 이 「인증 방식 미확정」이라고 적혀 있습니다 | 코워크 (노션) |
| 49 | claude | **`REQUESTS #1` 을 제가 성급히 닫았습니다(2026-08-20).** 「인증 방식 미확정」을 `POST /auth/guest` 가 200 이라는 이유로 해소 처리했는데, 그것이 답한 것은 **게스트가 JWT 를 쓴다**까지였고 **회원이 어떻게 인증하는가**는 열린 채였습니다. 이철희 님이 `NOW-AUTH-002`·`003` 구현을 시작하며 그 자리를 짚었습니다. 위 #48 으로 확정했습니다. **부정 진술만이 아니라 「해소」 표시도 근거를 적어야 한다**는 것이 남은 교훈입니다 | 기록 (조치 완료) |
| 50 | claude | **안내문 세 표의 `NOT NULL` 셋을 `NULL` 허용으로 바꿨습니다(2026-08-20).** `care_notes.from_name` · `care_note_rules.name` · `care_note_rules.keywords` — `NOW-NOTE-002 PUT /me/care` 요청에 셋 다 없어서 넣을 방법이 없었습니다. **지어내지 않고 비웁니다.** `db/patch_note_tables_v1.sql` 로 적용했습니다. `docs/schema_v63.sql` 은 제가 못 고칩니다 — 반영 부탁드립니다. ⚠️ **`keywords` 가 비면 케어 코치가 그 주의사항을 못 찾습니다** — 「각질 관리 해도 되나요」가 「안내문에 없습니다」로 갑니다. 데모 안내문에는 키워드가 있어 시연은 그대로 됩니다 | 코워크 (`docs/` 정정) |

### #4 상세 — Gradle 워커 argfile 인코딩

**증상** — `./gradlew build` 에서 `compileJava` · `compileTestJava` 까지 전부 통과한 뒤 `:test` 에서만 실패합니다.

```
Error: Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain
Caused by: java.lang.ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain
```

**원인** — Gradle 8.10.2 는 테스트 워커의 클래스패스를 `@argfile` 로 넘기는데, 이 파일을 **UTF-8** 로 씁니다.
그런데 워커 JVM 런처는 `@argfile` 을 **`sun.jnu.encoding`** 으로 읽습니다. 한국어 Windows 는 이 값이 `MS949`(ANSI 코드페이지 949)입니다.
그래서 경로의 한글이 깨집니다.

```
Gradle 이 쓴 것 (UTF-8) : C:\Users\송원석\.gradle\caches\8.10.2\workerMain\gradle-worker.jar
JVM 이 읽은 것 (MS949)  : C:\Users\?≪썝??\.gradle\caches\8.10.2\workerMain\gradle-worker.jar
```

없는 경로가 되어 `gradle-worker.jar` 를 못 찾습니다.

**확인 두 가지 (2026-08-17)**

1. 같은 argfile 을 MS949 로 다시 써서 워커를 띄우면 **클래스가 정상 적재**됩니다
2. 저장소를 영문 경로(`C:\Users\Public\nowbuild`)로 복사하고 `GRADLE_USER_HOME` 도 영문으로 두면
   **`./gradlew build` 가 `:test` · `:check` 까지 `BUILD SUCCESSFUL`** 입니다 (버전 무변경)

→ **코드·빌드 설정에는 문제가 없습니다.** 경로 인코딩만의 문제입니다.

**영향 범위** — 사용자 폴더나 프로젝트 경로에 한글이 있는 사람만. 지금은 JUnit 테스트가 0건이라 `:test` 가 하는 일이 없지만, **실제 테스트를 추가하는 순간 그 사람은 `./gradlew build` 를 못 돌립니다.**

**선택지 (지어내지 않고 넘깁니다 — 셋 다 `build.gradle` 또는 PC 설정을 건드립니다)**

| # | 방법 | 대가 |
|---|---|---|
| A | Windows 「Unicode UTF-8 사용(베타)」 켜서 ANSI 코드페이지를 65001 로 | 저장소 무변경. **PC 전체 설정**이라 다른 프로그램에 영향 가능 |
| B | `gradle.properties` 에 `org.gradle.jvmargs=-Dfile.encoding=COMPAT` + `build.gradle` 에 `options.encoding = 'UTF-8'` | 저장소 변경. 세 사람 전부에게 적용됨. macOS 는 COMPAT=UTF-8 이라 무해 |
| C | 프로젝트와 `GRADLE_USER_HOME` 을 영문 경로로 이동 | 저장소 무변경. 각자 로컬에서 옮겨야 함 |

**claude 는 손대지 않았습니다.** 「컴파일 오류만 고친다 · 버전과 설계를 건드리지 않는다」 범위 밖이고, `build.gradle` 은 이철희 님 소유라서입니다.

## 해결됨

| # | 무엇 | 답 | 언제 |
|---|---|---|---|
| — | `GET /today` 가 AI 를 부르는가 | **부릅니다.** AI 호출은 네 곳 | 2026-08-14 |
| — | `PATCH /me` 를 누가 맡는가 | 이철희의 `auth/` 로 통합 | 2026-08-14 |

## 쓰는 법

- **답을 지어내지 않습니다.** 여기에 적고 그 부분만 건너뜁니다
- 코드에는 `// TODO(REQUESTS #3): 프롬프트 확정 후 교체` 처럼 번호를 남깁니다
- 30분 이상 막히면 **더 파지 말고** 여기에 적고 종료합니다
