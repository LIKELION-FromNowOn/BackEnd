-- ============================================================================
--  마스터 시드 패치 — 명세 우선 4건  (2026-08-20)
--
--  이미 seed_master_v1 을 넣어 둔 DB 를 위한 것입니다.
--  새로 까는 DB 는 seed_master_v1.sql 만 넣으면 됩니다 — 이 패치가 이미 반영돼 있습니다.
--
--  왜 UPDATE 인가 — care_note_rules 가 care_items 를 참조하고 있어
--  시드를 통째로 다시 넣으면 DELETE 가 외래키에 막힙니다.
--
--  무엇을 고치나 — 노션 NOW-MASTER-002 예시와 다른 네 건입니다.
--    cr1 · cr2  빈도 편집 가능 + 매일
--    mv1        base 1.6 → 4.0 · minutes 10 → 60   (헬스장 가기)
--    mm1        minutes 5 → 3                       (처방약 복용)
--
--  minutes 는 today 의 durationSec(= minutes x 60) 이 됩니다.
--  화면의 「N분」·타이머·AI 문장 길이가 전부 이 값을 따릅니다.
--
--  적용  mysql --default-character-set=utf8mb4 -u <계정> -p fromnowon_db < db/patch_master_spec4_v1.sql
--  몇 번을 돌려도 같은 결과입니다.
-- ============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

UPDATE care_items SET core = 4.0, base = 2.0, minutes = 4,
       frequency_editable = TRUE, default_frequency = 'daily'
 WHERE id = 'cr1';
UPDATE care_items SET core = 5.0, base = 2.0, minutes = 4,
       frequency_editable = TRUE, default_frequency = 'daily'
 WHERE id = 'cr2';
UPDATE care_items SET core = 3.0, base = 4.0, minutes = 60,
       frequency_editable = TRUE, default_frequency = 'weekly_4plus'
 WHERE id = 'mv1';
UPDATE care_items SET core = 5.0, base = 1.0, minutes = 3,
       frequency_editable = FALSE, default_frequency = NULL
 WHERE id = 'mm1';

COMMIT;

-- ── 확인 ────────────────────────────────────────────────────────────────────
SELECT '빈도 편집 가능' 항목, COUNT(*) 행, 12 기대 FROM care_items WHERE frequency_editable
UNION ALL SELECT '소수 있는 행', COUNT(*), 6 FROM care_items
     WHERE core <> ROUND(core) OR base <> ROUND(base)
UNION ALL SELECT 'mv1 60분', COUNT(*), 1 FROM care_items WHERE id='mv1' AND minutes=60
UNION ALL SELECT 'mm1 3분',  COUNT(*), 1 FROM care_items WHERE id='mm1' AND minutes=3
UNION ALL SELECT 'cr1 매일', COUNT(*), 1 FROM care_items
     WHERE id='cr1' AND frequency_editable AND default_frequency='daily'
UNION ALL SELECT 'cr2 매일', COUNT(*), 1 FROM care_items
     WHERE id='cr2' AND frequency_editable AND default_frequency='daily';
