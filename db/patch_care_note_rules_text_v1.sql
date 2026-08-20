-- ============================================================================
--  care_note_rules 에 caution_text 한 칸  (2026-08-20)
--
--  NOW-NOTE-001 · 002 (김민정 님) 을 위한 것입니다.
--
--  왜 name 을 쓰면 안 되나 —
--    name 은 「문지르는 세안」 같은 짧은 라벨입니다.
--    제 GET /me/care/note 의 rules[].name 으로 이미 나가고 있습니다.
--
--    NOW-NOTE-001 의 cautions[].text 는 문장입니다.
--      「3일간 각질·고기능성 관리는 피해 주세요」
--    두 값이 용도가 다릅니다. 한 칸에 넣으면 둘 중 하나가 틀어집니다.
--
--  PUT /me/care 가 받은 text 를 그대로 넣고 그대로 돌려주면 됩니다.
--  서버가 조립하지 않습니다.
--
--  ★ 이 마이그레이션은 남의 앱을 깨뜨리지 않습니다.
--    ddl-auto: validate 는 엔티티가 요구하는 컬럼만 봅니다. NULL 허용이라 기존 행도 그대로입니다.
--
--  적용  mysql --default-character-set=utf8mb4 -u <계정> -p fromnowon_db < db/patch_care_note_rules_text_v1.sql
-- ============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'care_note_rules'
      AND column_name = 'caution_text') = 0,
  'ALTER TABLE care_note_rules ADD COLUMN caution_text VARCHAR(255) NULL COMMENT ''화면에 그대로 보여 줄 주의 문장. PUT /me/care 가 받은 값'' AFTER name',
  'SELECT ''caution_text 이미 있음''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 데모 안내문에도 문장을 채워 둡니다. 없으면 화면이 빕니다.
UPDATE care_note_rules r
  JOIN care_note_lines l ON l.care_note_id = r.care_note_id AND l.sent_no = r.sent_no
   SET r.caution_text = l.text
 WHERE r.caution_text IS NULL;

-- ── 확인 ────────────────────────────────────────────────────────────────────
SELECT '컬럼 caution_text' 항목, COUNT(*) 행, 1 기대 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'care_note_rules' AND column_name = 'caution_text'
UNION ALL SELECT '문장이 빈 규칙', COUNT(*), 0 FROM care_note_rules WHERE caution_text IS NULL
UNION ALL SELECT '규칙 행 수 유지', COUNT(*), COUNT(*) FROM care_note_rules;
