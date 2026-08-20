-- ============================================================================
--  시연용 클리닉 안내문 시드 — 1건
--
--  작성 2026-08-20 · Claude Code · 승인 송원석 (DISCREPANCIES-0819 PM 상신 ①)
--  출처 코워크 작업/output/jigeumbuteo_react_kit.zip → src/data/carenote.ts
--       ★ 값을 지어내지 않았습니다. 위 파일의 6문장 5규칙을 그대로 옮겼습니다.
--
--  ─────────────────────────────────────────────────────────────────────────
--  이걸 왜 넣는가 — 발표 차별점①이 「말」이 아니라 「화면」이 됩니다
--
--    G02 덜어내기 결과 · 「판정 안 함」 필터에 두 줄이 나란히 뜹니다.
--
--      클리닉 사후관리 가이드   판정 안 함   이 항목은 앱이 판단하지 않습니다
--                                            (cr6 · floor = excluded · medical)
--      고기능성 · 각질 관리     판정 안 함   클리닉에서 3일간 피하라고 하셨고,
--                                            하루 남았습니다  (cr4 · clinicNote)
--
--    앞은 「앱이 원래 안 건드리는 것」, 뒤는 「원래는 건드리는데 클리닉이 막은 것」입니다.
--    둘이 같이 보이면 차별점 설명이 한 화면에서 끝납니다.
--
--  ─────────────────────────────────────────────────────────────────────────
--  ⚠️ 먼저 있어야 하는 것 — 없으면 실행이 실패합니다
--
--    1. care_items 마스터 시드 (김민정 님)
--       care_note_rules.care_item_id 가 care_items(id) 를 참조합니다.
--       cr3 · cr4 가 없으면 외래키 오류가 납니다.
--       → 없으면 아래 @strict 를 0 으로 두십시오. 규칙은 들어가고 항목 연결만 비워 둡니다.
--
--    2. 대상 사용자
--       기본값은 「가장 최근에 만들어진 사용자」입니다.
--       시연 직전에 POST /auth/guest 를 부른 뒤 이 파일을 실행하면 그 사람에게 붙습니다.
--
--  ─────────────────────────────────────────────────────────────────────────
--  쓰는 법
--
--    mysql --default-character-set=utf8mb4 -u nowapp -p fromnowon_db < seed_carenote_demo.sql
--
--    특정 사용자에게 붙이려면 아래 @uid 줄을 직접 채우십시오.
--
--  되돌리기 — 맨 아래 「지우기」 절의 세 줄을 실행하면 됩니다.
--  ============================================================================


-- ────────────────────────────────────────────────────────────────────────────
--  0. 설정
-- ────────────────────────────────────────────────────────────────────────────

-- ★ 이 줄을 지우지 마십시오.
--   DB 는 utf8mb4_unicode_ci 인데 세션 기본값이 다르면
--   문자열을 이어 붙이는 순간 "Illegal mix of collations" 로 실패합니다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 대상 사용자. 비워 두면 가장 최근 사용자에게 붙습니다.
SET @uid = (SELECT id FROM users ORDER BY created_at DESC LIMIT 1);

-- care_items 마스터가 들어가 있으면 1, 아직 비어 있으면 0
-- 1 이면 규칙이 cr3 · cr4 를 직접 가리키고, 0 이면 키워드 매칭만 씁니다.
SET @strict = (SELECT IF(COUNT(*) >= 2, 1, 0) FROM care_items WHERE id IN ('cr3','cr4'));

-- 시술받은 날. 오늘에서 이틀 전으로 둡니다.
--   → cr4 규칙이 dp=3 이므로 daysLeft = max(0, 3 - 2) = 1
--     화면에 「하루 남았습니다」가 뜹니다. 0 이면 제한이 풀려 시연이 안 됩니다.
SET @received = DATE_SUB(CURDATE(), INTERVAL 2 DAY);

SET @note_id = 'cn_DEMO0000000000000000000001';

SELECT CONCAT('대상 사용자 : ', IFNULL(@uid, '★ 없음 — POST /auth/guest 를 먼저 부르십시오')) AS '';
SELECT CONCAT('항목 연결   : ', IF(@strict = 1, 'cr3 · cr4 에 연결합니다',
                                   '★ care_items 가 비어 있어 연결을 비웁니다')) AS '';
-- DATE 를 그대로 이어 붙이면 콜레이션이 딸려오므로 DATE_FORMAT 으로 문자열을 만듭니다
SELECT CONCAT('시술일      : ', DATE_FORMAT(@received, '%Y-%m-%d'),
              '  (오늘 기준 D+2 · cr4 는 하루 남음)') AS '';


-- ────────────────────────────────────────────────────────────────────────────
--  1. 다시 넣기 전에 지웁니다 (여러 번 실행해도 안전)
-- ────────────────────────────────────────────────────────────────────────────

-- ★ 순서가 중요합니다. rules 를 먼저 지우십시오.
--
--   care_note_rules 에는 안내문 계통 외래키가 둘인데 하나만 CASCADE 입니다.
--     fk_care_note_rules_note  → care_notes(id)                ON DELETE CASCADE
--     fk_care_note_rules_line  → care_note_lines(id, sent_no)  ← CASCADE 없음
--
--   그래서 care_notes 를 먼저 지우면 lines 로 번지는데,
--   그 lines 를 rules 가 붙잡아 「Cannot delete or update a parent row」 로 실패합니다.
--   (2026-08-20 실측. REQUESTS #19 로 올려 두었습니다)

DELETE FROM care_note_rules WHERE care_note_id = @note_id;
DELETE FROM care_notes      WHERE id           = @note_id;
-- care_note_lines 는 care_notes 삭제 시 CASCADE 로 함께 지워집니다


-- ────────────────────────────────────────────────────────────────────────────
--  2. 안내문
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO care_notes (id, user_id, title, from_name, is_sample, received_at) VALUES
  (@note_id, @uid, '시술 후 사후관리 안내', 'AAC 클리닉 · 웰니스하우스', TRUE, @received);


-- ────────────────────────────────────────────────────────────────────────────
--  3. 원문 6문장 — 문장 번호가 「원문 보기」의 근거입니다
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO care_note_lines (care_note_id, sent_no, `text`) VALUES
  (@note_id, 1, '시술 후 이틀간은 미온수로만 세안하시고 문지르지 마십시오.'),
  (@note_id, 2, '시술 후 3일간 각질 제거와 고기능성 제품 사용을 피해 주십시오.'),
  (@note_id, 3, '시술 후 3일간 격한 운동과 땀이 많이 나는 활동을 피해 주십시오.'),
  (@note_id, 4, '시술 후 7일간 사우나 · 찜질방 · 고온 목욕을 피해 주십시오.'),
  (@note_id, 5, '2주간 직사광선을 피하시고 외출 시 자외선 차단제를 발라 주십시오.'),
  (@note_id, 6, '붉어짐이 5일 이상 이어지거나 통증이 있으면 내원해 주십시오.');

-- 6번 문장에는 규칙이 없습니다. 「내원하십시오」는 제한이 아니라 안내라서입니다.


-- ────────────────────────────────────────────────────────────────────────────
--  4. 규칙 5개 — 문장에서 뽑은 제한
--
--    dp 는 제한 일수(D+n)이고 daysLeft = max(0, dp - 경과일) 로 조회할 때 계산합니다.
--    한 항목이 여러 규칙에 걸리면 dp 가 가장 큰 것을 씁니다.
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO care_note_rules (id, care_note_id, sent_no, name, keywords, dp, care_item_id) VALUES
  ('cnr_DEMO000000000000000000001', @note_id, 1, '문지르는 세안',
   JSON_ARRAY('세안','클렌징','폼클렌징','이중세안','스크럽세안'),
   2,  IF(@strict = 1, 'cr3', NULL)),

  ('cnr_DEMO000000000000000000002', @note_id, 2, '각질 · 고기능성',
   JSON_ARRAY('각질','필링','스크럽','레티놀','앰플','고기능성','필링패드'),
   3,  IF(@strict = 1, 'cr4', NULL)),

  ('cnr_DEMO000000000000000000003', @note_id, 3, '격한 운동',
   JSON_ARRAY('운동','헬스','러닝','달리기','등산','필라테스','요가','수영','크로스핏','pt','피티','축구','농구','테니스','클라이밍'),
   3,  NULL),

  ('cnr_DEMO000000000000000000004', @note_id, 4, '사우나 · 찜질',
   JSON_ARRAY('사우나','찜질','찜질방','목욕','반신욕','온천','스파','한증막','뜨거운물'),
   7,  NULL),

  ('cnr_DEMO000000000000000000005', @note_id, 5, '직사광선 노출',
   JSON_ARRAY('등산','바다','해변','캠핑','야외','수영장','골프','피크닉','축제','물놀이','페스티벌','운동회'),
   14, NULL);


-- ────────────────────────────────────────────────────────────────────────────
--  5. 확인
-- ────────────────────────────────────────────────────────────────────────────

SELECT '── 들어간 것 ──' AS '';

SELECT CONCAT('안내문 ', COUNT(*), '건') AS '' FROM care_notes      WHERE id = @note_id;
SELECT CONCAT('문장   ', COUNT(*), '개') AS '' FROM care_note_lines WHERE care_note_id = @note_id;
SELECT CONCAT('규칙   ', COUNT(*), '개') AS '' FROM care_note_rules WHERE care_note_id = @note_id;

SELECT '── 오늘 걸리는 제한 (daysLeft > 0 이면 판정 제외) ──' AS '';

SELECT CONCAT('  ', r.name,
              '  ·  남은 일수 ', GREATEST(0, r.dp - DATEDIFF(CURDATE(), n.received_at)),
              '  ·  항목 ', IFNULL(r.care_item_id, '(키워드 매칭)'),
              '  ·  원문 ', r.sent_no, '번') AS ''
  FROM care_note_rules r
  JOIN care_notes n ON n.id = r.care_note_id
 WHERE r.care_note_id = @note_id
   AND GREATEST(0, r.dp - DATEDIFF(CURDATE(), n.received_at)) > 0
 ORDER BY r.dp DESC;

SELECT '── 시연에서 이 문장이 뜹니다 ──' AS '';

SELECT CONCAT('  「클리닉에서 ', r.dp, '일간 피하라고 하셨고, ',
              GREATEST(0, r.dp - DATEDIFF(CURDATE(), n.received_at)), '일 남았습니다」') AS ''
  FROM care_note_rules r
  JOIN care_notes n ON n.id = r.care_note_id
 WHERE r.care_note_id = @note_id AND r.sent_no = 2;


-- ────────────────────────────────────────────────────────────────────────────
--  6. 지우기 — 시연이 끝난 뒤
-- ────────────────────────────────────────────────────────────────────────────

-- DELETE FROM care_notes WHERE id = 'cn_DEMO0000000000000000000001';
--   lines 와 rules 는 ON DELETE CASCADE 로 함께 지워집니다.
