# 04 · 패키지 간 인터페이스 다섯 개

**남의 `Repository` 를 직접 부르지 않습니다.** 아래 다섯 개만 씁니다.

> **8/18 정정 — 사용자 번호는 `Long` 이 아니라 `String` 입니다.**
> DB 규약이 「접두어 + ULID」(`us_01H8X…`)이고 `schema_v63.sql` 이 22테이블의 PK·FK 를 전부 `text` 로
> 정의했습니다. `Long` 으로는 담을 수가 없습니다.
**8/14 에 스텁으로 먼저 커밋합니다.** 구현이 없어도 각자 끝까지 갈 수 있습니다.

| 인터페이스 | 만드는 사람 | 쓰는 사람 | 어디서 |
|---|---|---|---|
| `SafetyPort` | 송원석 | 이철희 · 김민정 | 직접 입력 저장 · 예정 추가 |
| `NoteRulePort` | 송원석 | 김민정 | 예정 충돌 판정 · 홈 |
| `VerdictPort` | 송원석 | 김민정 | 오늘의 행동 · 홈 |
| `ItemPort` | 이철희 | 송원석 | 판정 입력 |
| `CheckinPort` | 이철희 | 송원석 | 판정 입력 |

## 시그니처

```java
// safety/SafetyPort.java — 송원석 제공
public interface SafetyPort {
    SafetyResult check(String text, Source source);

    record SafetyResult(boolean blocked, String message, List<String> hits) {}
    enum Source { ITEM_CUSTOM, SIGNAL_CUSTOM, COACH, PLAN, NOTE }
}
```

```java
// note/NoteRulePort.java — 송원석 제공
public interface NoteRulePort {
    List<NoteRule> activeRules(String userId);

    record NoteRule(int sentenceNo, int daysPeriod, String name,
                    List<String> keywords, String itemId, int daysLeft) {}
}
```

```java
// subtract/VerdictPort.java — 송원석 제공
public interface VerdictPort {
    Optional<VerdictSet> of(String userId, LocalDate date);
    Summary summary(String userId, LocalDate date);      // 홈이 쓰는 얇은 것

    record VerdictSet(List<ItemVerdict> results) {}
    record ItemVerdict(String itemId, String verdict,
                       String reason, String excludedBy) {}   // excludedBy: medical | clinicNote | null
    record Summary(int keep, int simplify, int reduce,
                   int skip, int excluded) {}
}
```

```java
// item/ItemPort.java — 이철희 제공
public interface ItemPort {
    List<SelectedItem> selected(String userId);

    record SelectedItem(String itemId, String frequency,
                        int floor, int evidenceLevel) {}
}
```

```java
// checkin/CheckinPort.java — 이철희 제공
public interface CheckinPort {
    Optional<LatestCheckin> latest(String userId);

    record LatestCheckin(String state, List<String> signalIds,
                         double signalStrength, LocalDateTime at) {}
}
```

## 왜 `VerdictPort` 에 `summary()` 가 따로 있는가

홈은 판정 32건 전부가 아니라 **개수 다섯 개**만 필요합니다.
같은 메서드를 쓰면 홈이 열릴 때마다 32건이 통째로 옵니다. **응답 다이어트를 인터페이스 수준에서 강제하는 장치**입니다.

## `GET /home` 은 규약으로 처리합니다

홈은 다섯 조각을 읽는 유일한 자리입니다. **각 패키지가 홈에 줄 조각을 스스로 만들어 둡니다.**

| 조각 | 만드는 패키지 |
|---|---|
| 관리 맥락 카드 | `care/` |
| 오늘의 케어 | `today/` |
| 내 상태 | `checkin/` |
| 덜어내기 요약 | `subtract/` |
| 첫 발자국 카드 id | `footstep/` (id 만. 본문은 클라이언트 캐시) |

각 패키지가 `xxxForHome(String userId)` 를 하나씩 열고, `HomeService` 는 그것들을 모으기만 합니다.

## 설계로 없앤 호출 네 곳

| 없앤 것 | 어떻게 |
|---|---|
| 홈 → 첫 발자국 | 카드 전체 대신 `footstepId` 만 |
| 오늘 → 관리 항목 | 판정 결과에 이미 `itemId` 가 있음 |
| `GET /me` 와 `PATCH /me` 분리 | `auth/` 로 통합 |
| 오늘 → 기록 쓰기 | `actions.completed_at` 파생 (**미확정 · 08 참고**) |
