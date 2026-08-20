/* 프로토타입 HTML 에서 CATS · ITEMS · SIGS 세 배열을 그대로 뽑아
   db/tools/source/master_source.mjs 로 씁니다.

   왜 저장소 안에 두는가 —
   원본 HTML 은 저장소 밖(코워크 작업/output/)에 있습니다.
   그 파일이 없으면 시드 생성기가 아예 못 돕니다.
   시드를 다시 만들 수 없다는 뜻이라 저장소 안에 사본을 둡니다.

   사용법: node db/tools/extract_source.mjs <프로토타입 HTML 경로>          */

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';

const SRC = process.argv[2];
if (!SRC) { console.error('사용법: node extract_source.mjs <jigeumbuteo_app_v6.3.html>'); process.exit(1); }

const lines = readFileSync(SRC, 'utf8').split('\n');

/* 대괄호 깊이가 0으로 돌아오는 줄까지가 배열 하나입니다. */
const grab = (name) => {
  const i = lines.findIndex((l) => l.trim().startsWith('var ' + name + '='));
  if (i < 0) throw new Error(name + ' 를 찾지 못했습니다');
  let depth = 0;
  const out = [];
  for (const l of lines.slice(i)) {
    out.push(l);
    depth += (l.split('[').length - 1) - (l.split(']').length - 1);
    if (depth === 0 && out.join('').includes('[')) break;
  }
  return out.join('\n');
};

const header =
  '/* 프로토타입 jigeumbuteo_app_v6.3.html 에서 그대로 뽑은 원본 데이터입니다.\n' +
  '   손으로 옮겨 적지 않았습니다 — db/tools/extract_source.mjs 가 뽑습니다.\n' +
  '   이 파일을 고치지 마십시오. 원본이 바뀌면 다시 뽑습니다. */\n\n';

const body = [grab('CATS'), grab('ITEMS'), grab('SIGS')].join('\n\n') +
             '\n\nexport { CATS, ITEMS, SIGS };\n';

mkdirSync('db/tools/source', { recursive: true });
writeFileSync('db/tools/source/master_source.mjs', header + body, 'utf8');

/* 뽑고 나서 개수를 셉니다. 통과만 보고 믿지 않습니다. */
const m = await import('../../db/tools/source/master_source.mjs?t=' + Date.now());
const got = { CATS: m.CATS.length, ITEMS: m.ITEMS.length, SIGS: m.SIGS.length };
const want = { CATS: 7, ITEMS: 32, SIGS: 14 };
for (const k of Object.keys(want)) {
  if (got[k] !== want[k]) { console.error(`${k} 가 ${got[k]} 건입니다. ${want[k]} 이어야 합니다`); process.exit(1); }
}
console.log(`  뽑음 — CATS ${got.CATS} · ITEMS ${got.ITEMS} · SIGS ${got.SIGS}`);
