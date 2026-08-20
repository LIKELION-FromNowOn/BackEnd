/*
 * 첫 발자국 시드 생성기 — db/seed_footstep_v1.sql 을 만듭니다.
 *
 * 왜 손으로 안 쓰는가
 *   8건 x 8칸입니다. 옮기다 한 글자를 놓쳐도 SQL 은 통과하고,
 *   화면에 그 문장이 그대로 뜹니다. 아무도 못 알아챕니다.
 *
 * 출처
 *   src/data/footsteps.ts  STEPS 8건 (jigeumbuteo_app_v6.3.html 463~ 과 같은 값)
 *   id 와 온보딩 4건은 노션 NOW-STEP-001 이 정한 것입니다
 *     "id": "fs_101" · "onboardingIds": ["fs_101","fs_104","fs_107","fs_108"]
 *
 * 실행
 *   node db/tools/gen_footstep_seed.mjs <데이터폴더> > db/seed_footstep_v1.sql
 */
import { readFileSync } from 'node:fs';

const DIR = process.argv[2];
if (!DIR) { console.error('사용법: node gen_footstep_seed.mjs <src/data 경로>'); process.exit(1); }

const load = (f) => readFileSync(`${DIR}/${f}`, 'utf8').replace(/export const /g, 'var ');
const { STEPS } = eval(load('footsteps.ts') + ';({STEPS})');

/* 노션 NOW-STEP-001 이 정한 온보딩 4건. 배열 순서로 1·4·7·8 번째입니다 */
const ONBOARDING = new Set(['fs_101', 'fs_104', 'fs_107', 'fs_108']);

/* p1 -> fs_101 … p8 -> fs_108. 명세 예시의 fs_101 이 p1(새벽 3시)과 같습니다 */
const idOf = (i) => `fs_${101 + i}`;

/* 작은따옴표만 처리합니다. 원본에 백슬래시가 없는 것을 확인했고,
   그래도 들어오면 조용히 깨지는 대신 여기서 멈춥니다. */
const q = (s) => {
  const t = String(s);
  if (t.indexOf(String.fromCharCode(92)) >= 0) {
    console.error(`✗ 백슬래시가 들어왔습니다: ${t}`); process.exit(1);
  }
  return "'" + t.split("'").join("''") + "'";
};

const out = [];
const p = (s = '') => out.push(s);

p('-- ============================================================================');
p('--  첫 발자국 시드 v1 — 8건');
p('--');
p('--  ★ 손으로 고치지 마십시오. db/tools/gen_footstep_seed.mjs 가 만듭니다.');
p('--');
p('--  출처   src/data/footsteps.ts (STEPS 8건)');
p('--  id     노션 NOW-STEP-001 이 정한 fs_101 ~ fs_108');
p('--         ⚠️ docs/07-response-rules.md:58 은 fs3 로 적혀 있는데 노션이 우선입니다');
p('--  온보딩  fs_101 · fs_104 · fs_107 · fs_108 — 명세의 onboardingIds 그대로');
p('--');
p('--  적용   mysql --default-character-set=utf8mb4 -u <계정> -p fromnowon_db < db/seed_footstep_v1.sql');
p('--         ★ --default-character-set=utf8mb4 를 빠뜨리면 한글이 깨집니다');
p('--');
p('--  몇 번을 돌려도 같은 결과입니다 (기존 행을 지우고 다시 넣습니다).');
p('-- ============================================================================');
p();
p('SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;');
p();
p('START TRANSACTION;');
p();
p('DELETE FROM footsteps;');
p();
p('-- next_steps 는 JSON 컬럼입니다. 문자열이 아니라 배열로 넣습니다 —');
p('-- 문자열로 넣으면 화면에서 파싱이 깨집니다.');
p('INSERT INTO footsteps');
p('  (id, category_id, title, who, situation, first_step, next_steps, quote, is_onboarding) VALUES');

const rows = STEPS.map((s, i) => {
  const id = idOf(i);
  const steps = s.th.map(q).join(', ');
  return `  (${q(id)}, ${q(s.cat)}, ${q(s.t)}, ${q(s.who)},\n`
       + `   ${q(s.bf)},\n`
       + `   ${q(s.fs)},\n`
       + `   JSON_ARRAY(${steps}),\n`
       + `   ${q(s.q)}, ${ONBOARDING.has(id) ? 'TRUE' : 'FALSE'})`;
});
p(rows.join(',\n') + ';');
p();
p('COMMIT;');
p();
p('-- ── 확인 ────────────────────────────────────────────────────────────────────');
p("SELECT '첫 발자국' 항목, COUNT(*) 행, 8 기대 FROM footsteps");
p("UNION ALL SELECT '온보딩', COUNT(*), 4 FROM footsteps WHERE is_onboarding");
p("UNION ALL SELECT '그다음 3건씩', COUNT(*), 8 FROM footsteps");
p("     WHERE JSON_LENGTH(next_steps) = 3");
p("UNION ALL SELECT 'id 형식 fs_1xx', COUNT(*), 8 FROM footsteps WHERE id REGEXP '^fs_10[1-8]$';");

console.log(out.join('\n'));

const on = STEPS.filter((_, i) => ONBOARDING.has(idOf(i)));
console.error(`첫 발자국 ${STEPS.length}건 · 온보딩 ${on.length}건`);
console.error('온보딩 — ' + on.map(s => s.cat).join(' · '));
console.error('그다음 3건이 아닌 것: ' + STEPS.filter(s => s.th.length !== 3).length);
