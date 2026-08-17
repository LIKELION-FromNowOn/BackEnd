# 03 · 직접 입력 텍스트 해석

**API** `NOW-ITEM-003` `POST /me/items/custom`
**패키지** `item/` · **API 소유자 이철희** · 프롬프트 소유자 송원석

> **이철희 님께** — 이 파일의 문자열을 그대로 쓰시면 됩니다. 호출 코드는 `item/` 안에 쓰시고,
> LLM 클라이언트는 `common/llm/` 것을 쓰십시오.

> **이 경로를 사전 생성으로 대체하지 마십시오.** 실시간 LLM 호출을 유지해야
> 발표에서 「AI 없어도 되는 것 아닌가요」에 답할 수 있습니다.

---

## 호출 전에 반드시 하는 것

```java
SafetyPort.SafetyResult r = safetyPort.check(text, Source.ITEM_CUSTOM);
if (r.blocked()) {
    // LLM 을 부르지 않습니다. 항목도 만들지 않습니다.
    return 상담안내(r.message());
}
```

**위기 신호 검사가 LLM보다 먼저입니다.** 프롬프트로 막으려 하지 마십시오.

---

## 서버가 채워 넣는 입력

```json
{
  "text": "주 2회 클라이밍",
  "categories": ["care", "life", "move", "food", "sleep", "mind"],
  "frequencies": ["weekly_1", "weekly_2", "weekly_3", "weekly_4plus", "daily"]
}
```

**허용 값 목록을 매번 함께 보냅니다.** 목록을 프롬프트에 하드코딩하면 마스터 데이터가 바뀔 때 갈라집니다.

---

## 시스템 프롬프트

```
당신은 사용자가 자유롭게 입력한 한 줄을 구조화된 항목으로 바꾸는 해석기입니다.

주어진 문장에서 다음 두 가지만 뽑습니다.
1. category — 반드시 입력으로 받은 categories 목록 안의 값 하나
2. frequency — 반드시 입력으로 받은 frequencies 목록 안의 값 하나, 또는 null

카테고리 판단 기준
- care  : 피부, 세안, 보습, 자외선 차단, 홈케어
- life  : 물 마시기, 영양제, 생활 습관 등 위에 안 맞는 생활 항목
- move  : 운동, 스트레칭, 걷기, 활동
- food  : 식사, 식단, 먹는 것
- sleep : 수면, 취침, 낮잠
- mind  : 명상, 일기, 마음 돌봄

빈도 판단 기준 (명세서 값 다섯 개뿐입니다. 이 밖의 값을 만들지 마십시오)
- 주 1회                            → weekly_1
- 주 2회                            → weekly_2
- 주 3회                            → weekly_3
- 주 4회 이상 / 거의 매일           → weekly_4plus
- 매일 / 하루에 / 아침저녁          → daily
- 문장에 빈도가 없으면              → null
- 「월 1회」처럼 다섯 개로 표현할 수 없는 빈도는 null 입니다. 가까운 값으로 옮기지 마십시오

절대 하지 않는 것
- 목록에 없는 값을 만들지 않습니다
- 문장을 고쳐 쓰거나 다듬지 않습니다. 원문은 서버가 그대로 저장합니다
- 판단이 서지 않는데 그럴듯한 값을 고르지 않습니다. 아래 규칙을 따릅니다
- 의학적 해석을 하지 않습니다. 「처방약」이라는 단어가 있어도 category 만 고릅니다

판단이 안 될 때
- category 를 못 고르겠으면 "life" 를 고릅니다
- frequency 가 문장에 없으면 null 을 넣습니다. 추측하지 않습니다
  (빈도를 잘못 넣으면 판정 점수 전체가 틀어집니다. null 이 훨씬 안전합니다)

출력은 아래 JSON 하나뿐입니다. 설명, 인사말, 코드펜스를 붙이지 마십시오.
```

---

## 출력 스키마

```json
{
  "category": "move",
  "frequency": "weekly_2"
}
```

| 필드 | 제약 |
|---|---|
| `category` | 입력 `categories` 안의 값. **아니면 `life` 로 강제** |
| `frequency` | `weekly_1` \| `weekly_2` \| `weekly_3` \| `weekly_4plus` \| `daily` \| `null`. **밖의 값이 오면 `null` 로 강제** |

**서버가 반드시 다시 검사합니다.** 목록에 없는 값이 오면 위 기본값으로 갈아 끼우고 `interpretedBy` 를 `fallback` 으로 둡니다.

`itemId` · `name` · `floor` · `evidenceLevel` 은 서버가 채웁니다.

| 서버가 고정하는 값 | 값 | 이유 |
|---|---|---|
| `floor` | **항상 `optional`** | 사용자 입력을 생리적 필수로 취급할 수 없습니다 |
| `evidenceLevel` | `low` 또는 `none` | 직접 입력은 근거가 없습니다 |
| `name` | **입력 원문 그대로** | LLM이 다듬은 문장을 쓰지 않습니다 |

---

## 폴백 — LLM 실패 시

```
category      = "life"
frequency     = null
interpretedBy = "fallback"
HTTP          = 200 (503 아님)
```

> **항목은 만들어 줍니다.** 「해석에 실패해 기본값으로 등록했습니다」가 명세서의 `LLM_UNAVAILABLE` 문구인데,
> 실제로는 **등록을 성공시키고 사용자에게 빈도를 물어보는 편**이 낫습니다. 화면에서 빈도 칩을 띄워 주십시오.
> `frequency` 가 `null` 인 항목은 판정에서 `load` 를 못 구하므로, **판정 전에 반드시 채워져야 합니다.**

---

## 예시

| 입력 | `category` | `frequency` |
|---|---|---|
| 주 2회 클라이밍 | `move` | `weekly_2` |
| 아침에 물 한 컵 | `life` | `daily` |
| 마스크팩 | `care` | `null` |
| 자기 전 스트레칭 | `move` | `daily` |
| 한 달에 한 번 두피 관리 | `care` | `null` (월 단위 값이 없습니다) |
| 영양제 챙겨 먹기 | `life` | `null` |

---

## 검수 시 볼 것

- [ ] 문장에 빈도가 없는데 값이 들어오지 않는가 (**가장 흔한 실패**)
- [ ] 목록 밖의 값이 오지 않는가
- [ ] 원문이 다듬어져 돌아오지 않는가
- [ ] 위기 문장이 `SafetyPort` 를 지나쳐 LLM에 도달하지 않는가
