-- ============================================================================
--  user_items 직접 입력 항목 컬럼 2개  (2026-08-20)
--
--  NOW-ITEM-003 직접 입력 항목 추가를 위한 것입니다.
--
--  왜 컬럼이 필요한가 —
--    NOW-ITEM-001 GET /me/items 응답 예시가 직접 입력 항목에도 category 를 돌려줍니다.
--      { "itemId": "cu_001", "name": "주 2회 클라이밍", "category": "move", ... }
--    저장하지 않으면 만들 때는 move 였다가 다시 조회하면 life 가 됩니다.
--
--    interpreted_by 는 조회 API 에 없지만, evidence_level 을 되살리는 유일한 근거입니다.
--    노션 NOW-ITEM-003 예시가 llm -> low · fallback -> none 입니다.
--    이 한 칸이 없으면 나중에 그 구분을 복원할 방법이 없습니다.
--
--  ★ 이 마이그레이션은 남의 앱을 깨뜨리지 않습니다.
--    ddl-auto: validate 는 「엔티티가 요구하는 컬럼이 DB 에 있는가」만 봅니다.
--    엔티티에 필드를 안 넣은 사람에게는 여분 컬럼이 그냥 보이지 않습니다.
--    그래서 먼저 넣어 두어도 안전하고, 엔티티는 이철희 님이 필요할 때 넣으시면 됩니다.
--
--  적용  mysql --default-character-set=utf8mb4 -u <계정> -p fromnowon_db < db/patch_user_items_custom_v1.sql
--  몇 번을 돌려도 같은 결과입니다 (이미 있으면 건너뜁니다).
-- ============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- MySQL 8.0 에는 ADD COLUMN IF NOT EXISTS 가 없어 이렇게 씁니다.
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user_items'
      AND column_name = 'custom_category') = 0,
  'ALTER TABLE user_items ADD COLUMN custom_category VARCHAR(32) NULL COMMENT ''직접 입력 항목의 카테고리. LLM 이 문장에서 고른 값'' AFTER custom_name',
  'SELECT ''custom_category 이미 있음''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user_items'
      AND column_name = 'interpreted_by') = 0,
  'ALTER TABLE user_items ADD COLUMN interpreted_by VARCHAR(20) NULL COMMENT ''llm 또는 fallback. evidence_level 을 되살리는 근거'' AFTER custom_category',
  'SELECT ''interpreted_by 이미 있음''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 값 제한. 마스터 항목은 둘 다 NULL 이어야 합니다.
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'user_items'
      AND constraint_name = 'ck_user_items_custom') = 0,
  'ALTER TABLE user_items ADD CONSTRAINT ck_user_items_custom CHECK (
     (is_custom = TRUE) OR (custom_category IS NULL AND interpreted_by IS NULL))',
  'SELECT ''ck_user_items_custom 이미 있음''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'user_items'
      AND constraint_name = 'ck_user_items_interpreted') = 0,
  'ALTER TABLE user_items ADD CONSTRAINT ck_user_items_interpreted CHECK (
     interpreted_by IS NULL OR interpreted_by IN (''llm'', ''fallback''))',
  'SELECT ''ck_user_items_interpreted 이미 있음''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ── 확인 ────────────────────────────────────────────────────────────────────
SELECT '컬럼 custom_category' 항목, COUNT(*) 행, 1 기대 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user_items' AND column_name = 'custom_category'
UNION ALL SELECT '컬럼 interpreted_by', COUNT(*), 1 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user_items' AND column_name = 'interpreted_by'
UNION ALL SELECT 'CHECK 2개', COUNT(*), 2 FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'user_items'
    AND constraint_name IN ('ck_user_items_custom', 'ck_user_items_interpreted')
UNION ALL SELECT '기존 행 손실 없음', COUNT(*), COUNT(*) FROM user_items;
