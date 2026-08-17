# CLAIMS — 지금 누가 무엇을 잡고 있는가

**작업을 시작하기 전에 이 파일을 읽고, 자기 줄을 추가한 뒤 바로 push 하십시오.**
끝나면 자기 줄을 지웁니다. **여기 이름이 있는 폴더는 읽기만 합니다.**

## 선점 중

| 패키지 · 파일 | 누가 | 시작 시각 | 무엇을 |
|---|---|---|---|
| (비어 있음) | | | |

## 쓰는 법

```
1. git checkout develop && git pull origin develop
2. 이 표에 줄 추가
   | today | claude | 2026-08-14 15:20 | GET /today 5건 구현 |
3. git add .agent/CLAIMS.md
   git commit -m "[claude] claim: today"
   git push origin develop
4. git checkout -b feature/today        # 에이전트 이름을 붙이지 않습니다
```

## 규칙

- **한 번에 한 패키지.** 두 개를 동시에 잡지 않습니다
- **`common/` 은 아무도 선점하지 않습니다.** 이철희 님만 고칩니다
- **선점이 6시간 넘게 남아 있으면** 사람에게 확인하고 지웁니다. 세션이 끊긴 것일 수 있습니다
- 굳이 한 패키지를 둘이 나눈다면 **파일까지 적습니다** — `today/TodayRepository.java`
