/*
 * 마스터 시드 생성기 — db/seed_master_v1.sql 을 만듭니다.
 *
 * 왜 손으로 안 쓰고 생성기를 두는가
 *   32 x 11 = 352 칸을 손으로 옮기면 반드시 어딘가 틀립니다. 그리고 틀려도
 *   눈에 안 띕니다 — core 2.5 를 2 로 적어도 SQL 은 통과하고, 판정만 조용히
 *   뒤집힙니다. 원본이 바뀌면 이 파일을 다시 돌리십시오.
 *
 * 출처 (모두 「값을 지어내지 않았다」의 근거입니다)
 *   src/data/items.ts     CATS 7 · ITEMS 32   (jigeumbuteo_app_v6.3.html 349~462 와 동일)
 *   src/data/signals.ts   SIGS 14
 *   jigeumbuteo_app_v6.3.html:1212  minutes 계산식
 *
 * 실행
 *   node db/tools/gen_master_seed.mjs <데이터폴더> > db/seed_master_v1.sql
 */
/* 원본은 db/tools/source/master_source.mjs 에 있습니다.
   프로토타입 HTML 에서 db/tools/extract_source.mjs 가 뽑은 것이고,
   손으로 옮겨 적지 않는 것이 이 방식의 목적입니다.
   원본 HTML 이 바뀌면 extract_source 를 먼저 돌리십시오. */
import { CATS, ITEMS, SIGS } from './source/master_source.mjs';

/* ── 원본 v6.3.html:1212 그대로. 지어낸 값이 아닙니다 ──
   mn 이 있는 항목(6건)은 그대로 두고, 없는 26건은 id 문자코드 합으로 정합니다.
   무작위가 아니라 결정적입니다 — 같은 id 면 언제 돌려도 같은 값입니다. */
const T = [4, 5, 7, 10, 12, 15, 18];
const minutesOf = (it) => {
  if (it.mn) return it.mn;
  let s = 0;
  for (let j = 0; j < it.id.length; j++) s += it.id.charCodeAt(j);
  return T[s % T.length];
};

/* ── 명세 우선 4건 ─────────────────────────────────────────────────────────────
   노션 API 명세서 NOW-MASTER-002 의 예시가 원본 프로토타입과 다른 네 건입니다.
   자료 우선순위상 노션이 앞서고, 2026-08-20 김민정 님과 합의했습니다.

   minutes 를 고치는 이유 — today 의 durationSec 이 minutes x 60 입니다.
   화면의 「N분」과 타이머에 그대로 들어가고,
   prompts/today-action.txt:9 가 「durationSec 안에 끝나는 크기로 만듭니다」라
   AI 가 만드는 문장 길이까지 이 값이 정합니다.
   공식이 만드는 일곱 값(4·5·7·10·12·15·18)은 짧은 항목에는 맞지만
   「헬스장 가기 · 10분」처럼 원래 긴 항목에서는 말이 안 됩니다.

   나머지 26건은 원본 공식 그대로 둡니다. 명세에 개별 값이 없습니다. */
const SPEC = {
  cr1: { fq: 1, df: '매일' },   // 명세 frequencyEditable true · daily
  cr2: { fq: 1, df: '매일' },   // 〃
  mv1: { base: 4, mn: 60 },     // 명세 base 4 · minutes 60  (헬스장 가기)
  mm1: { mn: 3 },               // 명세 minutes 3            (처방약 복용)
};

/* 이 아래로는 FINAL 만 씁니다. SPEC 이 반영된 32건입니다.
   ITEMS 를 그대로 세면 사람이 보는 숫자가 SQL 과 달라집니다. */
const FINAL = ITEMS.map((r) => ({ ...r, ...(SPEC[r.id] || {}) }));

const FLOOR = { 2: 'essential', 1: 'recommended', 0: 'optional', '-1': 'excluded' };
const EVID  = { 1: 'high', 2: 'medium', 3: 'low' };
const FREQ  = { '주 1회': 'weekly_1', '주 2회': 'weekly_2', '주 3회': 'weekly_3',
                '주 4회 이상': 'weekly_4plus', '매일': 'daily' };

/* 작은따옴표만 처리합니다. 원본에 백슬래시가 없는 것을 확인했고,
   그래도 들어오면 조용히 깨지는 대신 여기서 멈춥니다. */
const q = (s) => {
  const t = String(s);
  if (t.indexOf(String.fromCharCode(92)) >= 0) {
    console.error(`✗ 백슬래시가 들어왔습니다: ${t}`); process.exit(1);
  }
  return "'" + t.split("'").join("''") + "'";
};
const must = (map, key, what, id) => {
  const v = map[key];
  if (v === undefined) { console.error(`✗ ${id}: ${what} 에 없는 값 ${JSON.stringify(key)}`); process.exit(1); }
  return v;
};

const out = [];
const p = (s = '') => out.push(s);

p('-- ============================================================================');
p('--  마스터 시드 v1 — 카테고리 7 · 관리 항목 32 · 이상 징후 14');
p('--');
p('--  ★ 손으로 고치지 마십시오. db/tools/gen_master_seed.mjs 가 만듭니다.');
p('--    원본이 바뀌면 생성기를 다시 돌리십시오. 손으로 고치면 다음 생성 때 사라집니다.');
p('--');
p('--  출처   src/data/items.ts (CATS · ITEMS) · src/data/signals.ts (SIGS)');
p('--         = jigeumbuteo_app_v6.3.html 349~462 · 527~ 과 같은 값입니다');
p('--  소요시간 jigeumbuteo_app_v6.3.html:1212 의 계산식 그대로');
p('--');
p('--  적용   mysql --default-character-set=utf8mb4 -u <계정> -p fromnowon_db < db/seed_master_v1.sql');
p('--         ★ --default-character-set=utf8mb4 를 빠뜨리면 한글이 깨집니다');
p('--');
p('--  몇 번을 돌려도 같은 결과입니다 (기존 행을 지우고 다시 넣습니다).');
p('--  ★ 다만 care_items 를 참조하는 user_items 가 있으면 삭제가 막힙니다.');
p('--    그때는 실사용 데이터가 이미 있다는 뜻이니, 지우지 말고 멈추십시오.');
p('-- ============================================================================');
p();
p('SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;');
p();
p('START TRANSACTION;');
p();
p('-- 외래키 방향의 역순으로 지웁니다 (자식 먼저).');
p('DELETE FROM care_items;');
p('DELETE FROM signals;');
p('DELETE FROM categories;');
p();

/* ── 1. 카테고리 7 ──
   아이콘(ic)은 화면 것이라 넣을 칸이 없습니다. 프론트가 items.ts 로 씁니다. */
p('-- ── 카테고리 7 ──────────────────────────────────────────────────────────────');
p('INSERT INTO categories (id, name, sort_order) VALUES');
p(CATS.map((c, i) => `  (${q(c.id)}, ${q(c.nm)}, ${i + 1})`).join(',\n') + ';');
p();

/* ── 2. 관리 항목 32 ── */
p('-- ── 관리 항목 32 ────────────────────────────────────────────────────────────');
p('--   core · base 는 DECIMAL(3,1) 입니다. 2.5 를 2 로 반올림하면 판정이 뒤집힙니다.');
p('--   minutes 는 today 의 durationSec(= minutes x 60) 이 됩니다. 타이머와 AI 문장 길이를 정합니다.');
p('--   cr1 · cr2 · mv1 · mm1 네 건은 노션 명세서 값입니다. 나머지 26건은 원본 공식입니다.');
p('INSERT INTO care_items');
p('  (id, category_id, name, floor, evidence_level, core, base, minutes,');
p('   frequency_editable, default_frequency) VALUES');
const rows = FINAL.map((it) => {
  const floor = must(FLOOR, it.floor, 'floor', it.id);
  const evid  = must(EVID,  it.g,     'evidence_level', it.id);
  const df    = it.df ? q(must(FREQ, it.df, 'default_frequency', it.id)) : 'NULL';
  const fe    = it.fq ? 'TRUE' : 'FALSE';
  return `  (${q(it.id)}, ${q(it.c)}, ${q(it.nm)}, ${q(floor)}, ${q(evid)}, ` +
         `${it.core.toFixed(1)}, ${it.base.toFixed(1)}, ${minutesOf(it)}, ${fe}, ${df})`;
});
p(rows.join(',\n') + ';');
p();

/* ── 3. 이상 징후 14 ──
   배열에 적힌 순서가 화면에 뜨는 순서입니다 (sig_13 · sig_14 가 앞). */
p('-- ── 이상 징후 14 ────────────────────────────────────────────────────────────');
p('--   sort_order 는 원본 배열 순서입니다. 피부 2건이 앞에 오는 것은 의도입니다.');
p('INSERT INTO signals (id, group_name, name, weight, sort_order) VALUES');
p(SIGS.map((s, i) => `  (${q(s.id)}, ${q(s.g)}, ${q(s.t)}, ${s.w}, ${i + 1})`).join(',\n') + ';');
p();
p('COMMIT;');
p();

/* ── 넣은 것이 맞는지 스스로 확인 ── */
p('-- ── 확인 ────────────────────────────────────────────────────────────────────');
p("SELECT '카테고리' 항목, COUNT(*) 행, 7 기대 FROM categories");
p("UNION ALL SELECT '관리 항목', COUNT(*), 32 FROM care_items");
p("UNION ALL SELECT '이상 징후', COUNT(*), 14 FROM signals");
p("UNION ALL SELECT '징후 가중치 합', SUM(weight), 25 FROM signals");
p("UNION ALL SELECT '하한선 essential', COUNT(*), 7 FROM care_items WHERE floor='essential'");
p("UNION ALL SELECT '하한선 excluded', COUNT(*), 3 FROM care_items WHERE floor='excluded'");
/* DECIMAL 이 SMALLINT 로 되돌아가면 여기서 0 이 나옵니다.
   mv1 은 base 가 1.6 에서 명세값 4.0 이 되어 빠졌습니다. cr3 · cr4 는 둘 다 소수입니다. */
p("UNION ALL SELECT '소수 있는 행', COUNT(*), 6 FROM care_items");
p("     WHERE core <> ROUND(core) OR base <> ROUND(base)");
p("UNION ALL SELECT '빈도 편집 가능', COUNT(*), 12 FROM care_items WHERE frequency_editable;");

console.log(out.join('\n'));

/* 사람이 볼 요약은 stderr 로. stdout 은 SQL 만 나가야 합니다. */
const n = (f) => FINAL.filter(f).length;   // ITEMS 가 아니라 FINAL 입니다
console.error(`카테고리 ${CATS.length} · 항목 ${FINAL.length} · 징후 ${SIGS.length}`);
console.error(`가중치 합 ${SIGS.reduce((a, s) => a + s.w, 0)}`);
console.error(`essential ${n(i => i.floor === 2)} · recommended ${n(i => i.floor === 1)} · ` +
              `optional ${n(i => i.floor === 0)} · excluded ${n(i => i.floor === -1)}`);
console.error(`소수 ${n(i => i.core % 1 || i.base % 1)}건 · 빈도 편집 ${n(i => i.fq)}건 · ` +
              `mn 명시 ${n(i => i.mn)}건 / 계산 ${n(i => !i.mn)}건`);
