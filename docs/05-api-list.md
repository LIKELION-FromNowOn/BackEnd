# 05 · API 36건 (+ 제외 1건)

**요청 헤더 · 바디 · 응답 JSON · 실패 코드는 노션 「API 명세서」의 각 상세 페이지에 있습니다.**
이 표는 무엇이 있고 누가 만드는지만 담습니다.

## 이철희 — 13건

| API ID | 메서드 | 엔드포인트 | 무엇 |
|---|---|---|---|
| NOW-AUTH-001 | POST | `/auth/guest` | 게스트 세션 발급 |
| NOW-AUTH-002 | POST | `/auth/signup` | 회원 등록 |
| NOW-AUTH-003 | POST | `/auth/login` | 로그인 |
| NOW-AUTH-004 | POST | `/auth/logout` | 로그아웃 |
| NOW-AUTH-005 | GET | `/me` | 내 정보 조회 |
| NOW-MY-001 | PATCH | `/me` | 프로필 수정 |
| NOW-ITEM-001 | GET | `/me/items` | 내 항목 조회 |
| NOW-ITEM-002 | PUT | `/me/items` | 내 항목 저장 (최소 3개) |
| NOW-ITEM-003 | POST | `/me/items/custom` | 직접 입력 항목 추가 · **AI** |
| NOW-ITEM-004 | DELETE | `/me/items/{itemId}` | 항목 삭제 |
| NOW-STATE-001 | POST | `/checkins` | 상태 체크 제출 |
| NOW-STATE-002 | GET | `/checkins/latest` | 최근 상태 조회 |
| NOW-STATE-003 | POST | `/state/transition` | 상태 전환 응답 |

## 김민정 — 17건

| API ID | 메서드 | 엔드포인트 | 무엇 |
|---|---|---|---|
| NOW-MASTER-001 | GET | `/categories` | 카테고리 7 |
| NOW-MASTER-002 | GET | `/care-items` | 관리 항목 32 |
| NOW-MASTER-003 | GET | `/signals` | 이상 징후 14 |
| NOW-STEP-001 | GET | `/footsteps` | 첫 발자국 8 (상세까지 한 번에) |
| NOW-LOG-001 | GET | `/logs` | 기록 조회 |
| NOW-LOG-002 | GET | `/logs/summary` | 기록 요약 · 잠금 진행률 |
| NOW-TODAY-001 | GET | `/today` | 오늘의 행동 조회 · **AI** |
| NOW-TODAY-002 | POST | `/today/reroll` | 다른 행동 요청 |
| NOW-TODAY-003 | POST | `/today/start` | 타이머 시작 |
| NOW-TODAY-004 | POST | `/today/complete` | 완료 처리 |
| NOW-TODAY-005 | POST | `/today/reject` | 거절 사유 기록 |
| NOW-NOTE-001 | GET | `/me/care` | 관리 맥락 조회 |
| NOW-NOTE-002 | PUT | `/me/care` | 관리 맥락 저장 |
| NOW-NOTE-004 | GET | `/me/plans` | 예정 목록 · 충돌 여부 |
| NOW-NOTE-005 | POST | `/me/plans` | 예정 추가 |
| NOW-NOTE-006 | DELETE | `/me/plans/{planId}` | 예정 삭제 |
| NOW-HOME-001 | GET | `/home` | 홈 집계 |

## 송원석 — 6건

| API ID | 메서드 | 엔드포인트 | 무엇 |
|---|---|---|---|
| NOW-SUB-001 | POST | `/subtract/evaluate` | 덜어내기 판정 · **AI** |
| NOW-SUB-002 | GET | `/subtract/result` | 판정 결과 조회 |
| NOW-SUB-003 | POST | `/subtract/{itemId}/revert` | 판정 되돌리기 |
| NOW-NOTE-003 | GET | `/me/care/note` | 안내문 원문 · 문장 번호 |
| NOW-COACH-001 | POST | `/coach/ask` | 케어 코치 질의 · **AI** |
| NOW-SAFE-001 | POST | `/safety/check` | 위기 신호 검사 |

## 제외 1건

| NOW-STEP-002 | GET | `/footsteps/{id}` | **만들지 않습니다.** 사례가 8건뿐이라 NOW-STEP-001 이 상세까지 내려줍니다 |
|---|---|---|---|

## 실패 코드

| 코드 | 언제 |
|---|---|
| `MIN_ITEMS_REQUIRED` | 관리 항목이 최소 개수 미만 |
| `TEXT_REJECTED` | 자유 입력에 위기 신호 |
| `NO_CHECKIN` | 상태 체크 없이 판정 요청 |
| `CANNOT_REVERT_EXCLUDED` | `excluded` 항목에 되돌리기 요청 (**추가 제안 · 미확정**) |
