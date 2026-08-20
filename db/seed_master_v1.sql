-- ============================================================================
--  마스터 시드 v1 — 카테고리 7 · 관리 항목 32 · 이상 징후 14
--
--  ★ 손으로 고치지 마십시오. db/tools/gen_master_seed.mjs 가 만듭니다.
--    원본이 바뀌면 생성기를 다시 돌리십시오. 손으로 고치면 다음 생성 때 사라집니다.
--
--  출처   src/data/items.ts (CATS · ITEMS) · src/data/signals.ts (SIGS)
--         = jigeumbuteo_app_v6.3.html 349~462 · 527~ 과 같은 값입니다
--  소요시간 jigeumbuteo_app_v6.3.html:1212 의 계산식 그대로
--
--  적용   mysql --default-character-set=utf8mb4 -u <계정> -p fromnowon_db < db/seed_master_v1.sql
--         ★ --default-character-set=utf8mb4 를 빠뜨리면 한글이 깨집니다
--
--  몇 번을 돌려도 같은 결과입니다 (기존 행을 지우고 다시 넣습니다).
--  ★ 다만 care_items 를 참조하는 user_items 가 있으면 삭제가 막힙니다.
--    그때는 실사용 데이터가 이미 있다는 뜻이니, 지우지 말고 멈추십시오.
-- ============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

-- 외래키 방향의 역순으로 지웁니다 (자식 먼저).
DELETE FROM care_items;
DELETE FROM signals;
DELETE FROM categories;

-- ── 카테고리 7 ──────────────────────────────────────────────────────────────
INSERT INTO categories (id, name, sort_order) VALUES
  ('care', '피부 · 홈케어', 1),
  ('sleep', '수면', 2),
  ('move', '운동', 3),
  ('eat', '식사 · 영양', 4),
  ('mind', '마음', 5),
  ('life', '일상 루틴', 6),
  ('med', '건강 관리', 7);

-- ── 관리 항목 32 ────────────────────────────────────────────────────────────
--   core · base 는 DECIMAL(3,1) 입니다. 2.5 를 2 로 반올림하면 판정이 뒤집힙니다.
--   minutes 는 today 의 durationSec(= minutes x 60) 이 됩니다. 타이머와 AI 문장 길이를 정합니다.
--   cr1 · cr2 · mv1 · mm1 네 건은 노션 명세서 값입니다. 나머지 26건은 원본 공식입니다.
INSERT INTO care_items
  (id, category_id, name, floor, evidence_level, core, base, minutes,
   frequency_editable, default_frequency) VALUES
  ('cr1', 'care', '아침 보습 루틴', 'essential', 'high', 4.0, 2.0, 4, TRUE, 'daily'),
  ('cr2', 'care', '외출 전 자외선 차단', 'essential', 'high', 5.0, 2.0, 4, TRUE, 'daily'),
  ('cr3', 'care', '저녁 저자극 세안', 'recommended', 'high', 2.5, 2.5, 5, FALSE, NULL),
  ('cr4', 'care', '고기능성 · 각질 관리', 'optional', 'medium', 2.5, 3.5, 7, FALSE, NULL),
  ('cr5', 'care', '홈케어 기록 남기기', 'optional', 'low', 2.0, 2.2, 3, FALSE, NULL),
  ('cr6', 'care', '클리닉 사후관리 가이드', 'excluded', 'high', 5.0, 1.0, 5, FALSE, NULL),
  ('sl1', 'sleep', '7시간 이상 자기', 'essential', 'high', 3.0, 3.0, 18, FALSE, NULL),
  ('sl2', 'sleep', '취침 시각 고정하기', 'essential', 'high', 5.0, 2.0, 4, FALSE, NULL),
  ('sl3', 'sleep', '자기 전 폰 안 보기', 'optional', 'medium', 3.0, 4.0, 5, FALSE, NULL),
  ('sl4', 'sleep', '기상 후 스트레칭', 'optional', 'low', 2.0, 2.0, 7, FALSE, NULL),
  ('mv1', 'move', '헬스장 가기', 'recommended', 'high', 3.0, 4.0, 60, TRUE, 'weekly_4plus'),
  ('mv2', 'move', '러닝', 'recommended', 'medium', 3.0, 1.2, 12, TRUE, 'weekly_3'),
  ('mv3', 'move', '홈트', 'recommended', 'low', 2.0, 1.0, 15, TRUE, 'daily'),
  ('mv4', 'move', '스트레칭 10분', 'recommended', 'low', 2.0, 1.0, 18, FALSE, NULL),
  ('mv5', 'move', '계단 이용하기', 'recommended', 'medium', 3.0, 1.0, 4, FALSE, NULL),
  ('et1', 'eat', '하루 세 끼 챙겨 먹기', 'essential', 'high', 3.0, 3.0, 4, FALSE, NULL),
  ('et2', 'eat', '아침 거르지 않기', 'essential', 'medium', 3.0, 1.0, 5, FALSE, NULL),
  ('et3', 'eat', '물 2L 마시기', 'essential', 'high', 2.0, 2.0, 7, FALSE, NULL),
  ('et4', 'eat', '영양제 챙기기', 'optional', 'low', 2.0, 3.0, 10, FALSE, NULL),
  ('et5', 'eat', '야식 안 먹기', 'recommended', 'medium', 2.0, 3.0, 12, FALSE, NULL),
  ('md1', 'mind', '명상', 'recommended', 'medium', 3.0, 0.8, 18, TRUE, 'daily'),
  ('md2', 'mind', '감사 일기 쓰기', 'optional', 'low', 2.0, 1.0, 4, FALSE, NULL),
  ('md3', 'mind', '산책', 'recommended', 'high', 3.0, 1.0, 5, TRUE, 'weekly_3'),
  ('md4', 'mind', '모임 나가기', 'optional', 'low', 2.0, 2.0, 7, TRUE, 'weekly_1'),
  ('lf1', 'life', '아침 루틴 5단계', 'optional', 'low', 2.0, 5.0, 4, FALSE, NULL),
  ('lf2', 'life', '책상 정리 매일', 'optional', 'low', 1.0, 2.0, 5, FALSE, NULL),
  ('lf3', 'life', '가계부 쓰기', 'optional', 'low', 2.0, 1.0, 7, TRUE, 'daily'),
  ('lf4', 'life', '독서', 'optional', 'low', 2.0, 1.0, 10, TRUE, 'daily'),
  ('lf5', 'life', '영어 공부', 'optional', 'medium', 2.0, 1.4, 12, TRUE, 'daily'),
  ('lf6', 'life', '방 청소', 'optional', 'low', 2.0, 2.0, 15, TRUE, 'weekly_2'),
  ('mm1', 'med', '처방약 복용', 'excluded', 'high', 5.0, 1.0, 3, FALSE, NULL),
  ('mm2', 'med', '병원 정기 검진', 'excluded', 'high', 5.0, 2.0, 7, FALSE, NULL);

-- ── 이상 징후 14 ────────────────────────────────────────────────────────────
--   sort_order 는 원본 배열 순서입니다. 피부 2건이 앞에 오는 것은 의도입니다.
INSERT INTO signals (id, group_name, name, weight, sort_order) VALUES
  ('sig_13', '피부', '피부가 당기거나 예민하다', 2, 1),
  ('sig_14', '피부', '평소 쓰던 제품이 따갑게 느껴진다', 2, 2),
  ('sig_01', '수면', '잠들기까지 오래 걸린다', 2, 3),
  ('sig_02', '수면', '아침에 일어나기가 유난히 힘들다', 2, 4),
  ('sig_03', '수면', '자다가 자꾸 깬다', 2, 5),
  ('sig_04', '마음', '하고 싶은 게 없다', 3, 6),
  ('sig_05', '마음', '쉬어도 회복되지 않는다', 1, 7),
  ('sig_06', '마음', '사소한 일에 예민해진다', 2, 8),
  ('sig_07', '마음', '집중이 오래 안 간다', 2, 9),
  ('sig_08', '관계', '사람 만나는 게 부담스럽다', 2, 10),
  ('sig_09', '관계', '연락에 답하기가 미뤄진다', 1, 11),
  ('sig_10', '생활', '최근 스크롤 시간이 늘었다', 1, 12),
  ('sig_11', '생활', '끼니를 자주 거른다', 2, 13),
  ('sig_12', '생활', '몸 어딘가가 계속 불편하다', 1, 14);

COMMIT;

-- ── 확인 ────────────────────────────────────────────────────────────────────
SELECT '카테고리' 항목, COUNT(*) 행, 7 기대 FROM categories
UNION ALL SELECT '관리 항목', COUNT(*), 32 FROM care_items
UNION ALL SELECT '이상 징후', COUNT(*), 14 FROM signals
UNION ALL SELECT '징후 가중치 합', SUM(weight), 25 FROM signals
UNION ALL SELECT '하한선 essential', COUNT(*), 7 FROM care_items WHERE floor='essential'
UNION ALL SELECT '하한선 excluded', COUNT(*), 3 FROM care_items WHERE floor='excluded'
UNION ALL SELECT '소수 있는 행', COUNT(*), 6 FROM care_items
     WHERE core <> ROUND(core) OR base <> ROUND(base)
UNION ALL SELECT '빈도 편집 가능', COUNT(*), 12 FROM care_items WHERE frequency_editable;
