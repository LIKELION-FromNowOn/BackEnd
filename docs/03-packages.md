# 03 · 패키지 14개와 소유자

자르는 기준은 **「같이 바뀌는 것끼리 묶는다」** 입니다. 화면 기준도, 테이블 기준도 아닙니다.

```
com.youin.now
├─ common/       이철희   응답 봉투 · 예외 · 토큰 필터 · @CurrentUser · LLM 클라이언트
├─ auth/         이철희   인증 · 내 정보 · 프로필    users · sessions
├─ item/         이철희   관리 항목                  user_items
├─ checkin/      이철희   오늘 상태 · 신호 강도      checkins · checkin_signals
│
├─ master/       김민정   고정 데이터 (읽기 전용)    categories · care_items · signals
├─ footstep/     김민정   첫 발자국 사례             footsteps
├─ log/          김민정   기록 · 요약               (actions 파생)
├─ today/        김민정   오늘의 행동               actions · action_rejections
├─ care/         김민정   관리 맥락 · 예정           care_contexts · plans
├─ home/         김민정   홈 집계                   (없음 — 모으기만)
│
├─ subtract/     송원석   덜어내기 판정              evaluations · evaluation_results
├─ note/         송원석   안내문 원문 · 규칙 추출    care_notes · care_note_lines · care_note_rules
├─ coach/        송원석   케어 코치                 coach_messages
└─ safety/       송원석   위기 신호 검사            safety_checks
```

## 패키지 하나의 내부

```
today/
├─ TodayController.java
├─ TodayService.java
├─ TodayRepository.java
├─ TodayPort.java          ← 남에게 열어 주는 인터페이스 (있을 때만)
├─ dto/
│   ├─ TodayRes.java
│   └─ RejectReq.java
└─ entity/
    ├─ Action.java
    └─ ActionRejection.java
```

| 규칙 | 내용 |
|---|---|
| 엔티티는 **테이블 주인 패키지**에 | `actions` 는 `today/` 안에 |
| DTO 는 **밖으로 안 나갑니다** | 남에게 줄 때는 `Port` 의 `record` 로 |
| `Repository` 는 **패키지 밖에서 안 부릅니다** | 반드시 `Service` 나 `Port` 를 거칩니다 |

## 왜 이렇게 잘랐는가 — 한 줄씩

| 패키지 | 왜 따로 |
|---|---|
| `common` | 셋이 **같은 모양의 응답**을 내려야 프론트가 한 번만 분기합니다 |
| `auth` | 사용자 신원은 모든 기능의 전제라 **가장 안쪽에 격리**합니다 |
| `item` | 「무엇을 관리하기로 했는가」는 **하루 사이클과 수명이 다릅니다** |
| `checkin` | **판정의 입력**입니다. 입력과 계산을 분리합니다 |
| `master` | **안 바뀌는 데이터**입니다. 분리해야 캐시를 걸 수 있습니다 |
| `footstep` | 마스터와 성격은 같은데 **문구가 계속 다듬어집니다.** 같이 두면 캐시가 통째로 무효화됩니다 |
| `log` | **읽기 전용 파생**입니다. 쓰는 쪽과 나누면 읽기 쿼리를 마음껏 고칩니다 |
| `today` | **판정 결과를 「행동 하나」로 바꾸는 자리**입니다. 규칙이 완전히 다릅니다 |
| `care` | 안내문 **해석(AI)** 과 **적용(코드)** 을 나눈 것 중 「적용」 쪽입니다 |
| `home` | 여러 패키지를 읽는 **유일한 자리**. 한 군데로 몰아 두면 볼 곳이 한 곳입니다 |
| `subtract` | 앱의 심장. **8단계 중 AI 는 ⑥ 하나뿐**이라는 구조를 지켜야 합니다 |
| `note` | **문장 번호가 케어 코치의 신뢰 구조 전부**입니다. 섞으면 번호가 어긋납니다 |
| `coach` | AI 를 부르는 두 번째 자리. 판정과 프롬프트 성격이 다릅니다 |
| `safety` | **자유 입력을 받는 모든 곳이 거쳐야** 합니다. 각자 만들면 한 곳이 빠집니다 |

## 착수 순서

`safety` → `common` · DB → `auth` → `master` → 나머지

`safety` 가 가장 먼저인 이유는 **두 곳(`item` · `care`)에서 이걸 부르기 때문**입니다.
