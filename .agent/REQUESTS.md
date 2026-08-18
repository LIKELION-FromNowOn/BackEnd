# REQUESTS — 막힌 것 · 사람에게 넘기는 것

에이전트가 **추측으로 진행하는 대신** 여기에 적습니다.
사람이 답을 채우고 `해결` 로 옮깁니다.

## 열려 있음

| # | 올린이 | 무엇이 막혔나 | 누가 답해야 하나 |
|---|---|---|---|
| 1 | 송원석 | 인증 방식(JWT · 세션 · 매직링크)이 미확정이라 `auth/` 를 시작할 수 없습니다 | 이철희 |
| 2 | 송원석 | `daily_logs` 테이블을 만들지, `actions.completed_at` 파생으로 갈지 | 이철희 |
| 3 | 송원석 | 프롬프트 4종 원문이 아직 없습니다. `today` · `item` 의 AI 호출부가 막힙니다 | 송원석 |
| 4 | claude | **경로에 한글이 있는 Windows 에서 `./gradlew test` 가 실패합니다.** 코드 문제가 아니라 빌드 환경 문제라 판단이 필요합니다 (아래 상세) | 이철희 (`build.gradle` 소유) |
| 5 | claude | **`AuthTokenFilter` → `CurrentUserArgumentResolver` 경로 미검증.** 필터 등록은 로그로 확인했으나(`authTokenFilter urls=[/*] order=1`), **요청 속성에 넣은 값을 리졸버가 실제로 꺼내는 것은 확인하지 못했습니다.** `@CurrentUser` 를 쓰는 엔드포인트가 아직 하나도 없어서입니다. **「검증됐다」고 착각하지 마십시오.** 판정 API 컨트롤러(`NOW-SUB-001~003`) 등 `@CurrentUser` 를 쓰는 첫 엔드포인트에서 확인 예정 | 송원석 |
| 6 | claude | **배포 서버의 MySQL 버전 미확인.** 스키마를 **8.0.16+** 기준으로 작성했습니다. `CHECK` 23개가 **8.0.15 이하에서는 조용히 무시**되어 잘못된 값이 그대로 저장됩니다. 콜레이션은 구버전에서도 살도록 `utf8mb4_unicode_ci` 를 썼습니다. **적용 후 확인 쿼리로 테이블 22 · 외래키 28 · CHECK 23 을 반드시 세어 보십시오** (파일 맨 끝) | 이철희 (배포) |
| 7 | claude | **`users.email` 유니크 조건의 의미가 바뀌었습니다.** 원본은 `UNIQUE (email) WHERE deleted_at IS NULL`(살아 있는 회원끼리만 유일)인데 **MySQL 에 부분 유니크 인덱스가 없어** 그냥 `UNIQUE (email)` 로 갔습니다. **탈퇴한 회원의 이메일도 계속 막습니다.** 지금은 탈퇴 API 가 없어 드러나지 않지만, 생기면 탈퇴 시 `email` 을 비우거나 별도 컬럼으로 옮겨야 합니다. **제가 정할 사안이 아니라 그대로 두고 올립니다** | 송원석 |
| 8 | claude | **H2 를 제거하고 로컬도 MySQL 을 쓰도록 바꿨습니다(8/18).** 이제 `ddl-auto: validate` 가 엔티티와 실제 테이블을 대조하므로 **스키마 불일치가 서버가 아니라 로컬에서 걸립니다.** 대신 **각자 PC 에 MySQL 8.0.16+ 와 `fromnowon_db` 가 있어야 `bootRun` 이 됩니다.** 절차는 `application-local.yml.example` 머리말 참고 | 전원 (각자 로컬) |

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
