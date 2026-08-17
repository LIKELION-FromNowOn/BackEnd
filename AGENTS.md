# AGENTS.md — 이 저장소에서 일하는 모든 에이전트가 먼저 읽는 파일

**프로젝트** 「지금부터 (now)」 V6.3 · 뷰티 루틴 백엔드
**팀** 멋쟁이사자처럼 14기 · 3조 「할래 말래 (You in)」
**최종 갱신** 2026-08-16

> 이 파일이 **단일 원본**입니다. `CLAUDE.md` 는 이 파일을 가리키기만 합니다.
> Codex 도 Claude Code 도 **작업을 시작하기 전에 이 파일을 끝까지 읽습니다.**

---

## 0. 30초 요약

| | |
|---|---|
| **무엇을 만드나** | 관리 루틴에서 **오늘 하지 않아도 되는 것을 덜어내 주는** 앱의 백엔드 |
| **스택** | **Java 21** · **Spring Boot 3.3.5** · PostgreSQL · JPA · **Gradle 8.10.2** |
| **구조** | 단일 모듈 + **기능(도메인)별 패키지 14개** |
| **마감** | **8/20 자정 기능 동결** · **8/21 10:00 제출** (이후 어떤 사정으로도 불가) |
| **사람** | 백엔드 3명(송원석·김민정·이철희) · 프론트 1명(황인서) · 디자인 1명(김지현) |
| **AI 호출 지점** | **네 곳** — 판정 근거 · 오늘의 행동 · 직접 입력 해석 · 케어 코치 |

### 버전 — 2026-08-16 팀 합의. 임의로 바꾸지 마십시오

| 무엇 | 값 | 어디에 적혀 있나 |
|---|---|---|
| **Java** | **21** | `build.gradle` 의 `toolchain` |
| Spring Boot | 3.3.5 | `build.gradle` |
| 의존성 관리 플러그인 | 1.1.6 | `build.gradle` |
| Gradle | 8.10.2 | `gradle/wrapper/gradle-wrapper.properties` |
| DB | PostgreSQL (로컬은 H2 메모리) | `src/main/resources/application.yml` |
| 패키지 루트 | `com.youin.now` | |
| API 기본 경로 | `/api/v1` | `application.yml` 의 `context-path` |

**Java 21 은 세 사람과 배포 서버 모두에 해당합니다.** 로컬이 21이어도 서버가 17이면
「지원되지 않는 클래스 파일 버전」으로 배포 당일에야 터집니다.
IntelliJ 를 쓰신다면 `Settings → Build Tools → Gradle → Gradle JVM` 도 21인지 확인하십시오.

---

## 1. 읽는 순서

에이전트는 **작업 시작 전에 아래 순서로** 읽습니다. 전부 읽을 필요는 없고, 자기가 손댈 패키지에 해당하는 것만 읽으면 됩니다.

| 순서 | 파일 | 언제 필요한가 |
|---|---|---|
| 1 | **이 파일 (`AGENTS.md`)** | 항상 |
| 2 | `.agent/CLAIMS.md` | 항상 — **다른 에이전트가 뭘 잡고 있는지** |
| 3 | `docs/00-project.md` | 도메인 용어가 낯설 때 |
| 4 | `docs/03-packages.md` | 어느 폴더에 뭘 쓸지 |
| 5 | `docs/04-ports.md` | 남의 패키지를 불러야 할 때 |
| 6 | `docs/05-api-list.md` | 만들 API 를 찾을 때 |
| 7 | `docs/06-engine.md` | `subtract` · `today` · `checkin` 을 만질 때 **필수** |
| 8 | `docs/07-response-rules.md` | 응답 DTO 를 설계할 때 |
| 9 | `docs/08-open-items.md` | **아직 안 정해진 것.** 여기 있는 건 마음대로 정하지 마십시오 |
| 10 | `docs/01-decisions.md` | 「왜 이렇게 되어 있지?」 싶을 때 |

---

## 2. 절대 규칙 — 어기면 제품이 위험해집니다

이 일곱 가지는 **협상 대상이 아닙니다.** 코드 리뷰가 아니라 안전 문제입니다.

| # | 규칙 |
|---|---|
| 1 | `floor: -1` 인 항목(처방약 · 정기 검진 · 클리닉 안내)은 **판정 자체를 건너뜁니다** |
| 2 | `floor: 2` (생리적 필수)는 **`simplify` 가 하한**입니다. `reduce` · `skip` 으로 내릴 수 없습니다 |
| 3 | **클리닉 안내가 앱 제안보다 우선합니다.** 안내문에 걸린 항목은 그날 `excluded` 입니다 |
| 4 | **위기 신호는 AI가 아니라 코드가 감지합니다.** 자유 입력 5경로 전부 저장 전에 `SafetyPort` 를 통과합니다 |
| 5 | **연속 달성일·달성률을 만들지 않습니다.** 못 한 날은 기록하지 않습니다 |
| 6 | **브랜드명·제품명을 응답에 넣지 않습니다** |
| 7 | **AI가 숫자를 지어내지 않습니다.** 모르면 모른다고 답합니다 |

`safety_checks` 테이블에 **원문을 저장하지 않습니다.** 해시와 걸린 키워드만 남깁니다.

---

## 3. 패키지 소유권 — 폴더 하나에 사람 하나

```
com.youin.now
├─ common/       이철희   응답 봉투 · 예외 · 토큰 필터 · @CurrentUser · 설정 · LLM 클라이언트
├─ auth/         이철희   NOW-AUTH-001~005 · NOW-MY-001
├─ item/         이철희   NOW-ITEM-001~004
├─ checkin/      이철희   NOW-STATE-001~003
│
├─ master/       김민정   NOW-MASTER-001~003
├─ footstep/     김민정   NOW-STEP-001
├─ log/          김민정   NOW-LOG-001·002
├─ today/        김민정   NOW-TODAY-001~005
├─ care/         김민정   NOW-NOTE-001·002·004·005·006
├─ home/         김민정   NOW-HOME-001
│
├─ subtract/     송원석   NOW-SUB-001~003
├─ note/         송원석   NOW-NOTE-003
├─ coach/        송원석   NOW-COACH-001
└─ safety/       송원석   NOW-SAFE-001
```

**소유자 아닌 사람의 폴더를 고치지 않습니다.** 필요하면 `.agent/REQUESTS.md` 에 요청만 적습니다.

`common/` 은 **이철희 님만** 고칩니다. 세 사람이 쓰는 유일한 공유 지점입니다.

---

## 4. 코드 규칙 넷

### 4-1. `User` 를 넘기지 않고 `Long userId` 만 넘깁니다

```java
// 이렇게
@GetMapping("/today")
public ApiResponse<TodayRes> get(@CurrentUser Long userId) { ... }

@Entity @Table(name = "actions")
public class Action {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false) private Long userId;   // @ManyToOne User 가 아님
}
```

`@ManyToOne User` 를 쓰면 14개 패키지가 전부 `auth.User` 를 import 하게 되고, 컬럼 하나 고칠 때 다 깨집니다.
DB 외래키는 그대로 겁니다. **끊는 것은 자바 객체 참조뿐**입니다.

### 4-2. 클래스 이름은 자기 패키지 이름으로 시작합니다

| 패키지 | 접두어 | 패키지 | 접두어 |
|---|---|---|---|
| `common` | `Api` · `Global` | `today` | `Today` · `Action` |
| `auth` | `Auth` · **`User`** | `care` | `Care` · `Plan` |
| `item` | `Item` | `home` | `Home` |
| `checkin` | `Checkin` | `subtract` | `Subtract` · `Evaluation` |
| `master` | `Master` · `CareItem` | `note` | `Note` |
| `footstep` | `Footstep` | `coach` | `Coach` |
| `log` | `Log` | `safety` | `Safety` |

**`User` 로 시작하는 이름은 `auth/` 만 씁니다. 예약어입니다.**

안 지키면 Git 충돌이 아니라 **스프링 부팅이 실패**합니다.

```
ConflictingBeanDefinitionException: bean name 'userService' ... conflicts
org.hibernate.DuplicateMappingException: Duplicate entity mapping User
```

### 4-3. 남의 `Repository` 를 직접 부르지 않습니다

`docs/04-ports.md` 의 인터페이스 다섯 개만 씁니다. 그 외에는 자기 패키지 안에서 끝냅니다.

### 4-4. 응답 봉투는 하나입니다

```json
성공  { "ok": true,  "data": { ... } }
실패  { "ok": false, "error": { "code": "MIN_ITEMS_REQUIRED", "message": "..." } }
```

엔티티를 그대로 직렬화하지 않습니다. **화면 기준으로 DTO 를 만듭니다.**

---

## 5. 브랜치와 커밋

**`main` · `develop` · `feature/*` 세 단계입니다. `master` 는 쓰지 않습니다.**

| 브랜치 | 무엇 | 누가 머지 |
|---|---|---|
| `main` | 배포되는 것만 — 8/16 · 8/20 · 8/21 세 번 | 이철희 |
| `develop` | 통합. **항상 빌드가 통과해야 합니다** | 전원 |
| `feature/<패키지>-<작업자>` | 작업 단위 | 소유자 |

작업 브랜치는 **패키지 이름 + 작업자 아이디**입니다.
**작업자는 사람입니다. 에이전트 이름(`claude` · `codex`)을 쓰지 않습니다.**
Claude Code 가 만들든 Codex 가 만들든, **그 브랜치의 주인인 사람의 아이디**를 붙입니다.

| 사람 | 아이디 |
|---|---|
| 송원석 | `swonseok` |
| 이철희 | (본인이 정합니다) |
| 김민정 | (본인이 정합니다) |

```
feature/subtract-swonseok      feature/coach-swonseok
feature/today-<김민정 아이디>    feature/item-<이철희 아이디>
```

**한 사람이 여는 `feature/*` 는 최대 3개까지입니다.** 그 이상이면 PR 이 흩어져 마감 전에 머지가 밀립니다.
네 번째가 필요하면 **먼저 하나를 `develop` 에 머지하고 지운 뒤** 여십시오.

**같은 브랜치에 두 에이전트가 커밋하지 않습니다.**

> **브랜치 이름이 에이전트를 구분하지 않으므로, 충돌을 막는 것은 `.agent/CLAIMS.md` 선점 하나뿐입니다.**
> Claude Code 와 Codex 가 같은 패키지를 잡으면 **둘 다 `feature/<패키지>-<같은 사람>` 을 만듭니다.**
> **선점을 건너뛰면 그대로 덮어씁니다.** 6장을 반드시 지키십시오.

커밋 메시지는 **접두어로 누가 했는지 남깁니다. 접두어는 「작업자 아이디」입니다** — 브랜치와 같은 원칙입니다.

**Claude Code 가 쓰든 Codex 가 쓰든, 그 작업의 주인인 사람의 아이디를 붙입니다.**
누가 실제로 타이핑했는지는 접두어가 아니라 `Co-Authored-By` 트레일러와 `.agent/WORKLOG.md` 가 남깁니다.

```
[swonseok] feat(subtract): 판정 파이프라인 8단계 구현
[swonseok] fix(safety): 위기 키워드 정규화 누락 수정
[swonseok] chore: gradle 의존성 추가
```

**아이디는 위 표와 같습니다.** 송원석 님 작업은 **전부 `[swonseok]`** 입니다.

> **이철희 · 김민정 님 접두어는 아직 정해지지 않았습니다.**
> **본인이 정하시면 됩니다.** 다른 사람이 대신 정하지 않습니다 — 정해지면 위 표에 채워 주십시오.
> 그때까지 두 분은 쓰시던 방식을 그대로 쓰셔도 됩니다.

> **접두어가 틀렸어도 이미 push 한 커밋은 고치지 마십시오.**
> 고치려면 이력 재작성 + force-push 가 필요한데 **아래 금지 4번에 걸립니다.**
> 다음 커밋부터 맞추면 됩니다. **이력은 되감지 않고 앞으로만 씁니다.**

### 하지 말 것

| # | 금지 | 이유 |
|---|---|---|
| 1 | `main` 에 직접 push | 배포본이 깨집니다 |
| 2 | `feature/*` 끼리 머지 | 이력이 엉킵니다. **반드시 `develop` 을 거칩니다** |
| 3 | `develop` 빌드를 깬 채로 종료 | 다음 사람이 멈춥니다 |
| 4 | **`git push --force` · `--force-with-lease` · 이력 재작성 전부** | 남의 커밋이 사라집니다. **예외 없습니다** |
| 5 | 남의 `feature/*` 에 커밋 | 소유자 규칙과 같습니다 |

---

## 6. 두 에이전트 공동 작업 규약

**Claude Code 와 Codex 는 서로의 존재를 모릅니다.** 같은 파일을 동시에 고치면 나중에 쓴 쪽이 앞의 것을 지웁니다.
그래서 **파일로 선점**합니다.

### 6-1. 세션을 시작할 때 — 네 단계

```bash
# 1. 최신 상태로
git checkout develop && git pull origin develop

# 2. 누가 뭘 잡고 있는지 확인
cat .agent/CLAIMS.md

# 3. 비어 있는 패키지를 하나 골라 선점 줄을 추가하고 바로 push
#    (CLAIMS.md 편집 → 커밋 → push. 이 커밋은 develop 에 바로 올립니다)
git add .agent/CLAIMS.md
git commit -m "[claude] claim: today"
git push origin develop

# 4. 작업 브랜치 생성 — 패키지 + 작업자 아이디 (에이전트 이름이 아닙니다)
git checkout -b feature/today-swonseok
```

**선점 줄 형식** (`.agent/CLAIMS.md`)

```
| today | claude | 2026-08-14 15:20 | GET /today 5건 구현 |
```

### 6-2. 작업하는 동안

| 규칙 | 내용 |
|---|---|
| **한 번에 한 패키지** | 두 패키지를 동시에 잡지 않습니다. 끝내고 다음을 선점합니다 |
| **선점 안 된 곳만** | `CLAIMS.md` 에 다른 에이전트 이름이 있으면 **그 폴더는 읽기만** 합니다 |
| **`common/` 은 둘 다 금지** | 필요하면 `.agent/REQUESTS.md` 에 적고 다음으로 넘어갑니다 |
| **`docs/` 수정 금지** | 설계 문서는 사람이 고칩니다. 틀린 걸 발견하면 `.agent/REQUESTS.md` 에 적습니다 |
| **30분 이상 막히면 멈춤** | 추측으로 진행하지 않습니다. `.agent/REQUESTS.md` 에 적고 종료합니다 |

### 6-3. 세션을 끝낼 때 — 네 단계

```bash
# 1. 빌드가 통과하는지 확인. 안 되면 push 하지 않습니다
./gradlew build

# 2. develop 에 머지
git checkout develop && git pull origin develop
git merge feature/today-swonseok
git push origin develop

# 3. CLAIMS.md 에서 자기 줄을 지웁니다
# 4. WORKLOG.md 에 무엇을 했는지 적고 같이 push
```

**작업 로그 형식** (`.agent/WORKLOG.md`)

```
## 2026-08-14 15:20~16:40 · claude · today
- GET /today · POST /today/reroll 구현
- VerdictPort 는 스텁 사용 (subtract 미완)
- 남은 것: POST /today/reject 의 none 분기
- 막힌 것: 없음
```

### 6-4. 충돌이 났다면

**혼자 풀지 마십시오.** 충돌은 「두 에이전트가 같은 곳을 잡았다」는 신호이고, 그건 선점 규칙이 지켜지지 않았다는 뜻입니다.

```
1. git merge --abort
2. .agent/REQUESTS.md 에 「A 패키지에서 충돌. 누가 잡고 있는지 확인 필요」 기록
3. 종료하고 사람에게 알립니다
```

### 6-5. 두 에이전트의 역할을 나눠 쓰는 방법 (권장)

한 패키지를 둘이 나눠 하지 말고, **패키지 단위로 번갈아** 가져가십시오.

| 방식 | 결과 |
|---|---|
| 같은 패키지를 둘이 | 충돌 · 중복 구현 · 서로 지움 |
| **패키지를 번갈아** | 충돌 0. 대신 진도는 두 배 |

굳이 한 패키지 안에서 나눈다면 **레이어가 아니라 파일 단위**로 나누십시오 — 「Codex 가 `TodayRepository` + 엔티티, Claude 가 `TodayService` + 컨트롤러」 처럼요. 그래도 `CLAIMS.md` 에 **파일까지** 적어야 합니다.

---

## 7. AI 호출은 네 곳입니다 — 프롬프트는 남이 줍니다

**LLM 을 부르는 자리가 네 곳 있고, 그 프롬프트는 전부 송원석이 만듭니다.**

| # | 어디 | API | 그 API 소유자 | 프롬프트 소유자 |
|---|---|---|---|---|
| 1 | 판정 근거 문장 생성 | `NOW-SUB-001` ⑥단계 | 송원석 | 송원석 |
| 2 | 오늘의 행동 생성 | `NOW-TODAY-001` | **김민정** | 송원석 |
| 3 | 직접 입력 텍스트 해석 | `NOW-ITEM-003` | **이철희** | 송원석 |
| 4 | 케어 코치 답변 | `NOW-COACH-001` | 송원석 | 송원석 |

**2번과 3번은 소유자가 다릅니다.** 그래서 규칙을 둡니다.

| 규칙 | 내용 |
|---|---|
| **프롬프트는 코드가 아니라 산출물로 넘깁니다** | 송원석이 문자열 · 폴백 문장 · 금지 표현을 파일로 줍니다. **남의 패키지에 코드를 쓰지 않습니다** |
| **호출은 패키지 소유자가 합니다** | 김민정이 `today/` 에서, 이철희가 `item/` 에서 각자 부릅니다 |
| **LLM 클라이언트는 `common/llm/` 하나** | 이철희가 만들고 셋이 씁니다. 각자 만들면 재시도·타임아웃·폴백이 세 벌이 됩니다 |

**프롬프트마다 반드시 있어야 하는 것 세 가지**

- **폴백 문장** — 호출이 실패했을 때 대신 나갈 문장. **빈 결과를 반환하지 않습니다**
- **금지 표현** — 브랜드명 · 제품명 · 의학적 단정
- **말투** — 정중한 평문형. 「~하세요」가 아니라 「~합니다 / ~해도 괜찮습니다」

> **에이전트에게** — 프롬프트 파일이 아직 없으면 **`docs/08-open-items.md` 를 확인하고, 임시 문자열을 지어내지 마십시오.**
> 폴백 문장만 넣고 TODO 를 남긴 뒤 `.agent/REQUESTS.md` 에 적으십시오.

---

## 7-1. 아직 정해지지 않은 것 — 마음대로 정하지 마십시오

`docs/08-open-items.md` 에 전체 목록이 있습니다. 코드에 직접 걸리는 것:

| 미결 | 어떻게 하나 |
|---|---|
| 인증 방식 (JWT · 세션 · 매직링크) | `sessions` 테이블 존재 여부가 걸립니다. **정해지기 전에는 `auth/` 를 만들지 마십시오** |
| `daily_logs` 테이블 유지 여부 | `actions.completed_at` 파생안이 제안돼 있습니다. **확정 전까지 `daily_logs` 를 만들지 마십시오** |
| 관리 항목 최소 개수 | 제안값 3. `MIN_ITEMS_REQUIRED` 의 기준입니다 |
| 다시 받기 한도 | `actions.reroll_count` 상한 |
| 상태 전환 재제안 유예 기간 | 예시 3일 |

**모르면 만들지 말고 `.agent/REQUESTS.md` 에 적으십시오.** 지어낸 값이 DB 에 들어가면 되돌리는 데 하루가 갑니다.

---

## 8. 확정된 것 — 이건 바꾸지 마십시오

| 항목 | 값 |
|---|---|
| 판정 값 5종 | `keep` · `simplify` · `reduce` · `skip` · `excluded` |
| `floor` 4등급 | `2` 필수 · `1` 권장 · `0` 선택 · `-1` 판정 제외 |
| 근거 등급 | `0` none · `1` low · `2` medium · `3` high |
| 판정 점수 | `score = core × 1.7 − load × 0.75 − strict × (load × 0.35)` |
| 부담 | `load = base + FQL[frequency]` · `load ≤ 1.5` 면 `keep` |
| 잠금 | 주간 7일 · 월간 30일 |
| 거절 사유 | `time` · `fit` · `none` |
| `nextStep` 검사 순서 | `onboarding → rest → checkin → subtract → action → done` |

자세한 것은 `docs/06-engine.md`.

---

## 9. 모르면 이렇게 하십시오

```
1. docs/ 에서 찾습니다
2. 없으면 노션을 확인합니다 (워크스페이스 「멋사 3조 해커톤 team_할래말래」)
3. 그래도 없으면 .agent/REQUESTS.md 에 적고 그 부분만 건너뜁니다
```

**추측해서 채우지 마십시오.** 이 프로젝트는 문서가 코드보다 먼저 나와 있습니다. 없다는 것은 아직 안 정했다는 뜻입니다.
