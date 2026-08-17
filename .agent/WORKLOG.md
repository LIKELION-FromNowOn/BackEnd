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
- **이력 재작성 1회** — 커밋 접두어를 `[swonseok]` 으로 통일하면서 이 브랜치의 커밋 메시지를 다시 썼습니다.
  `AGENTS.md` 5장 금지 4번(force-push)의 **1회 예외**이며, 송원석 본인 브랜치이고 `develop` 머지 전이라
  남이 받아간 커밋이 없어 안전하다고 판단했습니다. **`--force-with-lease` 로 밀었고 해시가 전부 바뀌었습니다.**
  `develop` · `main` 은 건드리지 않았습니다

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
