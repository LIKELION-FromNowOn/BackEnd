# 지금부터 (now) — 백엔드

멋쟁이사자처럼 14기 · 3조 「할래 말래 (You in)」 · AAC 트랙

관리 루틴에서 **오늘 하지 않아도 되는 것을 덜어내 주는** 앱의 백엔드입니다.

## 요구 사항

| 무엇 | 값 |
|---|---|
| **Java** | **21** (고정) |
| Gradle | 8.10.2 — `./gradlew` 를 쓰면 자동 |
| Spring Boot | 3.3.5 |
| DB | PostgreSQL. **로컬은 DB 없이도 뜹니다** (H2 메모리) |

```bash
java -version          # 21 이 아니면 빌드가 안 됩니다
./gradlew build
./gradlew bootRun      # http://localhost:8080/api/v1
```

## 시작하기

| 대상 | 먼저 읽을 것 |
|---|---|
| **사람** | `docs/00-project.md` → `docs/02-roles.md` |
| **AI 에이전트 (Claude Code · Codex)** | **`AGENTS.md`** — 끝까지 |

## 문서

| 파일 | 내용 |
|---|---|
| `AGENTS.md` | 에이전트 규칙 **단일 원본** |
| `CLAUDE.md` | Claude Code 전용 메모 (규칙은 AGENTS.md) |
| `docs/00-project.md` | 무엇을 만드는가 · 용어 |
| `docs/01-decisions.md` | 확정된 결정과 그 근거 |
| `docs/02-roles.md` | 역할 분배 31.4 / 34.3 / 34.3 |
| `docs/03-packages.md` | 패키지 14개와 소유자 |
| `docs/04-ports.md` | 패키지 간 인터페이스 5개 |
| `docs/05-api-list.md` | API 36건 목록 |
| `docs/06-engine.md` | 판정 엔진 규칙 |
| `docs/07-response-rules.md` | 응답 설계 규칙 |
| `docs/08-open-items.md` | 아직 안 정해진 것 |
| `docs/09-session-log.md` | 여기까지 온 과정 |
| `docs/10-subtract-impl.md` | 판정 파이프라인 구현 노트 |
| `docs/prompts/00~04` | 프롬프트 4종 + 공통 규칙 |
| `.agent/CLAIMS.md` | 누가 무엇을 잡고 있는가 |
| `.agent/WORKLOG.md` | 작업 기록 |
| `.agent/REQUESTS.md` | 막힌 것 · 요청 |

## 브랜치

`main` · `develop` · `feature/*` — **`master` 는 쓰지 않습니다.**

## 마감

| 날짜 | |
|---|---|
| 8/20 자정 | **기능 동결.** 이후 버그 수정만 |
| 8/21 10:00 | **제출.** 이후 어떤 사정으로도 불가 |
