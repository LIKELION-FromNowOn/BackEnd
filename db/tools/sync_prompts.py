# -*- coding: utf-8 -*-
"""docs/prompts/*.md 의 시스템 프롬프트 블록을 classpath 리소스로 뽑습니다.

손으로 옮기면 문서와 코드가 갈라집니다. 프롬프트가 바뀌면 이 스크립트를 다시 돌리십시오.

    python db/tools/sync_prompts.py
"""
import io, re, sys, pathlib

# 네 개 전부 뽑습니다. 02 는 김민정 님(today/), 03 은 이철희 님(item/) 것이지만
# 리소스로 뽑아 두면 두 분이 같은 방식으로 쓰실 수 있습니다.
# 문서가 원본이고 이 파일들은 사본입니다 — 프롬프트를 고치면 문서를 고치고 이걸 다시 도십시오.
PAIRS = [
    ('docs/prompts/01-subtract-reason.md', 'src/main/resources/prompts/subtract-reason.txt'),
    ('docs/prompts/02-today-action.md',    'src/main/resources/prompts/today-action.txt'),
    ('docs/prompts/03-item-interpret.md',  'src/main/resources/prompts/item-interpret.txt'),
    ('docs/prompts/04-coach-answer.md',    'src/main/resources/prompts/coach-answer.txt'),
]

def extract(md_path):
    """## 시스템 프롬프트 바로 다음의 ``` 블록을 통째로 가져옵니다."""
    s = io.open(md_path, encoding='utf-8').read()
    m = re.search(r'##\s*시스템 프롬프트\s*\n+```[a-z]*\n(.*?)\n```', s, re.S)
    if not m:
        print(f'  ✗ {md_path} 에서 시스템 프롬프트 블록을 못 찾았습니다')
        sys.exit(1)
    return m.group(1).strip() + '\n'

for md, out in PAIRS:
    body = extract(md)
    pathlib.Path(out).parent.mkdir(parents=True, exist_ok=True)
    io.open(out, 'w', encoding='utf-8', newline='\n').write(body)
    print(f'  {out}  ({len(body.splitlines())}줄)  ← {md}')
