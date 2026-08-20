# 10 · 판정 파이프라인 구현 노트 (2026-08-15)

**`subtract/` 와 `safety/` 에 코드가 들어갔습니다.** 스프링·DB·LLM 없이 도는 순수 로직입니다.
**동작 확인 31건을 통과했습니다** — `src/test/java/.../SubtractPipelineCheck.java`

---

## 들어간 것

| 파일 | 무엇 |
|---|---|
| `SubtractVerdict` | 판정 5종 + 하한선 비교 + 되돌리기 가능 여부 |
| `SubtractFloor` | 하한선 4등급. 마스터의 문자열(`essential`)과 프로토타입 숫자(2)를 한 곳에 묶음 |
| `SubtractFrequency` | 빈도 5종 + FQL 가중치 |
| `SubtractCondition` | 상태 5단계 + `strict` + 여력값 + `judgeStrength` |
| `SubtractItem` | 마스터 + 사용자 빈도. `load()` 를 여기서 계산 |
| `ClinicCaution` | 안내문 제한 하나. `daysLeft` 를 **저장하지 않고 계산** |
| `SubtractResult` | 항목 하나의 결과 |
| `SubtractEngine` | 점수 계산. **순수 함수** |
| `SubtractValidator` | ⑦단계 서버 측 검증 8가지 |
| `SubtractPipeline` | ①~⑧ 전체 |
| `SafetyKeywords` · `SafetyService` | 위기 신호 검사 (규칙 기반, AI 아님) |

---

## 확인된 것 31건 (요약)

| 묶음 | 확인 내용 |
|---|---|
| 계산 | `load = base + FQL`, 점수식이 명세서와 일치, `load ≤ 1.5 → keep` |
| **빈도 누락** | **빈도가 비면 예외를 던집니다.** 기본값을 지어내지 않습니다 |
| 하한선 | **모든 상태 × 중요도 × 빈도 조합에서 `essential` 이 하한 아래로 안 갑니다** |
| 8단계 | ② `floor` → ③ 클리닉 → ④ 근거없음 → ⑤ 되돌림 순서대로 걸러짐 |
| 안내문 | 기간이 지나면 더는 막지 않음 |
| **LLM 실패** | **예외가 나도 결과가 나오고, 전부 폴백 문장으로 채워집니다** |
| 검증 8가지 | 의학적 단정·하한선 위반·판정제외 항목·미선택 항목·근거없음이 전부 잡힘 |
| 상태 | `unknown` 이 판정을 막지 않음. `drained` 만 추천 중단 |
| **결정성** | **50회 반복 결과가 전부 같습니다** (무작위 요소 없음) |

---

## 두 가지 정정 — 프로토타입과 다르게 갔습니다

### 1. 클리닉 제한 항목은 `drop` 이 아니라 `excluded`

프로토타입 `engine.ts` 는 클리닉 안내에 걸린 항목을 `'drop'`(= `skip`)으로 돌려줍니다.
**API 명세서 `NOW-SUB-001` 응답 예시는 `excluded` + `excludedBy: "clinicNote"` 입니다.**

→ **명세서를 따랐습니다.** `drop` 이면 사용자에게 「오늘은 쉬세요」로 보이고 되돌리기 버튼이 뜨는데,
클리닉 안내는 **앱이 판단하지 않는 영역**이라 되돌리기가 있으면 안 됩니다.

### 2. 서버 측 검증 5번의 대상

「근거 등급 0 인데 판정이 있는가」는 **되돌린 항목이 아니라 근거 없는 항목**입니다.
둘을 헷갈리지 않도록 검사를 `[5]`(근거 없음)와 `[5-1]`(되돌림)로 나눴습니다.

---

## 아직 안 한 것 — 남의 것이거나 미확정이라

| 무엇 | 왜 안 했나 |
|---|---|
| `SubtractRepository` · 저장(⑧) | **DB 스키마 대기.** 파이프라인은 DB 를 모르게 짜 두어서, 저장 코드만 붙이면 됩니다 |
| `VerdictPort` 실구현 | 위와 같음. 지금은 `VerdictPortStub` 이 「오늘 판정 없음」을 돌려줍니다 |
| LLM 호출부 | **`common/llm/` 은 이철희 님 소유.** 파이프라인은 `ReasonGenerator` 인터페이스로 주입받습니다 |
| 컨트롤러 · `ApiResponse` 포장 | **`common/` 의 응답 봉투 대기** |
| 위기 키워드 확정본 | **김민정 님 대기.** 지금은 프로토타입 11개가 `SafetyKeywords.DRAFT_11` 에 있습니다 |

---

## 붙이는 방법 (프로젝트가 올라온 뒤)

```java
// subtract/SubtractService.java — DB·LLM 이 준비되면 이 껍데기만 쓰면 됩니다
List<SubtractItem>  items    = itemPort.selected(userId).stream().map(this::toSubtractItem).toList();
SubtractCondition   cond     = SubtractCondition.of(checkinPort.latest(userId).orElseThrow().state());
List<ClinicCaution> cautions = noteRulePort.activeRules(userId).stream().map(this::toCaution).toList();

SubtractPipeline.Outcome out = SubtractPipeline.run(items, cond, cautions,
        (drafts, c) -> llmClient.ask(PROMPT_01, toJson(drafts, c), ReasonMap.class).reasons());

repository.saveAll(out.results());          // ⑧
log.info("검증 교정 {}건 {}", out.validationFixes().size(), out.validationFixes());
```

**`ReasonGenerator` 가 `null` 이면 전부 폴백 문장으로 돕니다.** LLM 이 없어도 데모가 삽니다.

---

## 돌려 보는 법

**프로젝트가 올라온 지금은 그래들로 돌립니다.**

```bash
./gradlew build -x test testClasses

# Windows
java -cp "build/classes/java/test;build/classes/java/main" com.youin.now.subtract.SubtractPipelineCheck
# macOS · Linux
java -cp "build/classes/java/test:build/classes/java/main" com.youin.now.subtract.SubtractPipelineCheck
```

**「통과 31 · 실패 0」이 나와야 정상입니다.** 8/17 에 실제로 확인했습니다.

> **`testClasses` 를 빼지 마십시오.** `-x test` 만 쓰면 Gradle 이 `compileTestJava` 까지 함께 건너뜁니다
> (그 태스크를 요구하는 것이 `test` 뿐이라서입니다). 그러면 확인용 클래스가 컴파일되지 않거나
> **낡은 클래스가 남아 엉뚱한 숫자가 나옵니다.** 8/18 에 실제로 30/1 이 나온 적이 있습니다.

> **JUnit 으로 옮기지 마십시오 — 8/20 기능 동결 전까지는.**
> 경로에 한글이 있는 PC 에서 `:test` 가 실패합니다(Gradle argfile 인코딩 문제).
> 지금은 JUnit 테스트가 0건이라 `:test` 가 할 일이 없어 아무 손해가 없는데,
> 테스트를 추가하는 순간 그 사람들의 빌드가 막힙니다. 자세한 것은 `HANDOFF.md` 12장 · `.agent/REQUESTS.md` #4.
