-- ============================================================================
--  안내문 세 표를 PUT /me/care 가 쓸 수 있게  (2026-08-20)
--
--  NOW-NOTE-002 (김민정 님) 이 네 군데에서 막힙니다.
--  전부 제 스키마 쪽이라 제가 엽니다.
--
--  ① care_notes.from_name  NOT NULL  →  NULL 허용
--     PUT /me/care 요청에 클리닉 이름이 없습니다.
--     사용자가 직접 적는 관리 맥락이라 발신처가 없는 것이 정상입니다.
--
--  ② care_note_rules.name  NOT NULL  →  NULL 허용
--     cautions[] 에 name 이 없습니다. 「문지르는 세안」 같은 라벨은
--     안내문을 사람이 읽고 붙인 것이고, 사용자 입력에는 없습니다.
--     지어내지 않고 비웁니다.
--
--  ③ care_note_rules.keywords  NOT NULL  →  NULL 허용
--     매칭용 키워드도 요청에 없습니다. NoteService.parseKeywords 가
--     NULL 을 빈 목록으로 처리합니다 — 확인했습니다.
--
--     ⚠️ 키워드가 없으면 케어 코치가 그 주의사항을 못 찾습니다.
--        「각질 관리 해도 되나요」 → 매칭 실패 → 「안내문에 없습니다」
--        데모 안내문에는 키워드가 있어 코치 시연은 그대로 됩니다.
--
--  ④ fk_care_note_rules_line 에 ON DELETE CASCADE  ← 이게 제일 큽니다
--     지금은 care_notes 를 지우면 이렇게 됩니다.
--       ERROR 1451: Cannot delete or update a parent row
--       fk_care_note_rules_line FOREIGN KEY (care_note_id, sent_no)
--     외래키가 셋인데 둘만 CASCADE 라 지우는 순서가 강제됩니다.
--     PUT /me/care 가 「통째로 갈아 끼우기」라 여기서 막힙니다.
--     .agent/REQUESTS.md #21 이 이 건입니다.
--
--  적용  mysql --default-character-set=utf8mb4 -u <계정> -p fromnowon_db < db/patch_note_tables_v1.sql
--  몇 번을 돌려도 같은 결과입니다.
-- ============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE care_notes      MODIFY from_name VARCHAR(100) NULL COMMENT '발신 클리닉. 사용자가 직접 적은 맥락이면 NULL';
ALTER TABLE care_note_rules MODIFY name      VARCHAR(255) NULL COMMENT '짧은 라벨. 사용자 입력이면 NULL';
ALTER TABLE care_note_rules MODIFY keywords  JSON         NULL COMMENT '매칭용 키워드. 없으면 코치가 못 찾습니다';

-- 외래키를 CASCADE 로 다시 겁니다. 이름이 같아 지우고 다시 만듭니다.
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE() AND constraint_name = 'fk_care_note_rules_line'
      AND delete_rule = 'CASCADE') = 0,
  'ALTER TABLE care_note_rules DROP FOREIGN KEY fk_care_note_rules_line',
  'SELECT ''이미 CASCADE''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE() AND constraint_name = 'fk_care_note_rules_line') = 0,
  'ALTER TABLE care_note_rules ADD CONSTRAINT fk_care_note_rules_line
     FOREIGN KEY (care_note_id, sent_no) REFERENCES care_note_lines (care_note_id, sent_no)
     ON DELETE CASCADE',
  'SELECT ''외래키 그대로''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ── 확인 ────────────────────────────────────────────────────────────────────
SELECT 'from_name NULL 허용' 항목, COUNT(*) 행, 1 기대 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='care_notes' AND column_name='from_name' AND is_nullable='YES'
UNION ALL SELECT 'name NULL 허용', COUNT(*), 1 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='care_note_rules' AND column_name='name' AND is_nullable='YES'
UNION ALL SELECT 'keywords NULL 허용', COUNT(*), 1 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='care_note_rules' AND column_name='keywords' AND is_nullable='YES'
UNION ALL SELECT '문장 외래키 CASCADE', COUNT(*), 1 FROM information_schema.referential_constraints
  WHERE constraint_schema=DATABASE() AND constraint_name='fk_care_note_rules_line' AND delete_rule='CASCADE'
UNION ALL SELECT '데모 안내문 유지', COUNT(*), 1 FROM care_notes
UNION ALL SELECT '데모 규칙 유지', COUNT(*), 5 FROM care_note_rules;
