# CLAUDE.md

> **먼저 `AGENTS.md` 를 끝까지 읽으십시오.** 규칙의 단일 원본은 그 파일입니다.
> 이 파일에는 Claude Code 에만 해당하는 것만 적습니다. **규칙을 여기에 복사해 두지 않습니다** — 두 벌이 되면 반드시 갈라집니다.

## 이 저장소에서

- 커밋 메시지 접두어는 **`[claude]`** 입니다
- 작업 브랜치는 **`feature/<패키지>-<작업자 아이디>`** 입니다 — 예 `feature/subtract-swonseok`
  - **아이디는 사람 것입니다.** `claude` 를 붙이지 마십시오 (`feature/subtract-claude` 아님)
  - **한 사람당 `feature/*` 는 최대 3개.** 네 번째가 필요하면 하나를 머지하고 지운 뒤 여십시오
  - 이름이 에이전트를 구분하지 않으니 **`.agent/CLAIMS.md` 선점이 유일한 충돌 방지 장치**입니다. 건너뛰지 마십시오
- 작업 전 `.agent/CLAIMS.md` 를 읽고 선점 줄을 추가한 뒤 시작합니다
- **`docs/` 를 수정하지 않습니다.** 틀린 것을 발견하면 `.agent/REQUESTS.md` 에 적습니다

## 자주 쓰는 명령

```bash
java -version            # 21 이어야 합니다. 아니면 빌드가 안 됩니다
./gradlew build          # 빌드 + 테스트
./gradlew bootRun        # 실행 — DB 없이도 뜹니다 (local 프로파일 · H2 메모리)
./gradlew test           # 테스트만
```

**버전은 `AGENTS.md` 0장의 표가 원본입니다.** Java 21 · Spring Boot 3.3.5 · Gradle 8.10.2.
**임의로 올리거나 내리지 마십시오.** 필요하면 `.agent/REQUESTS.md` 에 적으십시오.

## Claude Code 를 쓸 때의 요령

- **한 번에 한 패키지만** 잡으십시오. 여러 폴더를 동시에 고치면 Codex 와 부딪힙니다
- 파일을 만들기 전에 `docs/03-packages.md` 에서 **그 폴더의 주인이 누구인지** 확인하십시오
- `docs/04-ports.md` 의 인터페이스 다섯 개 외에는 **남의 패키지를 부르지 않습니다**
- 값이 명세에 없으면 **지어내지 말고** `.agent/REQUESTS.md` 에 적고 그 부분만 건너뛰십시오
