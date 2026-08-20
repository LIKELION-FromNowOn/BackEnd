package com.youin.now.subtract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * 마스터 32건 전수 확인 — <b>하한선이 한 번이라도 뚫리는지</b> 봅니다.
 *
 * <p>{@link SubtractPipelineCheck} 는 손으로 만든 표본 31케이스입니다. 표본은 「내가 떠올린
 * 경우」만 봅니다. 이 검사는 반대로, <b>실제로 배포될 마스터 데이터 전부</b>를 넣고
 * 상태 5 × 빈도 5 를 모두 돌립니다 — <b>32 x 5 x 5 = 800 판정</b>입니다.
 *
 * <p>읽는 것은 DB 가 아니라 <b>커밋된 {@code db/seed_master_v1.sql} 그 자체</b>입니다.
 * MySQL 이 없어도 돌고, 「저장소에 올라간 값」을 검사한다는 점이 중요합니다.
 *
 * <h2>이 검사가 실제로 무언가를 잡는지 확인했습니다 (2026-08-20)</h2>
 *
 * <p><b>「통과 · 실패 0」만 보고 믿으면 안 됩니다.</b> 일부러 코드를 부숴 보고 실패가
 * 나오는지 확인했습니다. 결과가 뜻밖이었고, 그래서 적어 둡니다.
 *
 * <pre>
 *   엔진만 부숨   (ESSENTIAL -&gt; SKIP)          실패 0   ← 검증기가 되돌려 놓습니다
 *   검증기만 부숨 (하한선 보정 제거)              실패 0   ← 엔진이 애초에 안 뚫습니다
 *   둘 다 부숨                                 실패 30  ← 여기서 걸립니다
 * </pre>
 *
 * <p>즉 <b>하한선은 엔진과 검증기 두 곳이 각각 독립적으로 지킵니다.</b> 한 곳이 틀려도
 * 사용자에게는 안 나갑니다. 대신 <b>이 검사만으로는 어느 한쪽의 고장을 못 찾습니다</b> —
 * 두 곳을 따로 보려면 {@link SubtractValidator} 를 직접 부르는 검사가 따로 있어야 합니다.
 *
 * <p>판정 분포를 함께 찍는 이유도 같습니다. 전부 {@code keep} 이 나오면 하한선 검사는
 * 통과하지만 <b>아무것도 증명하지 못합니다.</b> 그래서 「덜어내기가 실제로 일어남」을
 * 단정문으로 넣어 두었습니다.
 *
 * <pre>
 * javac -encoding UTF-8 -d build/check $(find src -name '*.java' | grep -v Stub)
 * java -cp build/check com.youin.now.subtract.MasterFloorSweepCheck
 * </pre>
 */
public final class MasterFloorSweepCheck {

    static int pass = 0, fail = 0;
    static final List<String> failures = new ArrayList<>();

    static void check(String name, boolean ok, String detail) {
        if (ok) pass++;
        else { fail++; failures.add(name + "  -> " + detail); }
    }

    // ── 시드 SQL 파싱 ───────────────────────────────────────────────────────
    // 정규식을 쓰지 않습니다. 따옴표 안의 쉼표까지 정확히 다루려면 한 글자씩 보는 편이
    // 짧고 틀릴 데가 없습니다.
    static List<String> splitTop(String row) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        for (int i = 0; i < row.length(); i++) {
            char ch = row.charAt(i);
            if (ch == '\'') {                       // '' 는 따옴표 하나입니다
                if (inStr && i + 1 < row.length() && row.charAt(i + 1) == '\'') {
                    cur.append('\''); i++; continue;
                }
                inStr = !inStr; continue;
            }
            if (ch == ',' && !inStr) { out.add(cur.toString().trim()); cur.setLength(0); continue; }
            cur.append(ch);
        }
        out.add(cur.toString().trim());
        return out;
    }

    record Master(String id, String category, String name, SubtractFloor floor,
                  String evidence, double core, double base, int minutes,
                  boolean freqEditable, String defaultFrequency) {}

    static List<Master> readSeed(Path sql) throws IOException {
        List<Master> out = new ArrayList<>();
        boolean inItems = false;
        for (String raw : Files.readAllLines(sql, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.startsWith("INSERT INTO care_items")) { inItems = true; continue; }
            if (!inItems) continue;
            // 컬럼 목록 줄도 "(" 로 시작합니다. 값 줄은 "('" 로 시작하는 것으로 가릅니다.
            if (!line.startsWith("('")) continue;
            boolean last = line.endsWith(";");
            String body = line.substring(1, line.lastIndexOf(')'));
            List<String> f = splitTop(body);
            if (f.size() != 10) throw new IllegalStateException("칸 수가 10이 아닙니다: " + line);
            out.add(new Master(f.get(0), f.get(1), f.get(2), SubtractFloor.of(f.get(3)), f.get(4),
                    Double.parseDouble(f.get(5)), Double.parseDouble(f.get(6)),
                    Integer.parseInt(f.get(7)), "TRUE".equals(f.get(8)),
                    "NULL".equals(f.get(9)) ? null : f.get(9)));
            if (last) break;
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        Path sql = Path.of(args.length > 0 ? args[0] : "db/seed_master_v1.sql");
        List<Master> master = readSeed(sql);

        System.out.println("== 마스터 전수 확인 ==");
        System.out.println("  읽은 파일   " + sql);
        System.out.println("  관리 항목   " + master.size() + "건");

        check("항목 32건", master.size() == 32, "실제 " + master.size());

        // 빈도를 받지 않는 항목은 빈도를 바꿔도 결과가 같아야 하므로 조합에서 1가지만 셉니다.
        // 다만 800 이라는 수는 「32 x 상태 5 x 빈도 5」로 세는 것이 맞습니다 —
        // 빈도를 받지 않는 항목도 매 판정마다 함께 판정되기 때문입니다.
        int judged = 0;
        Map<SubtractFloor, Integer> perFloor = new EnumMap<>(SubtractFloor.class);
        // ★ 무엇이 실제로 나왔는지 세어 둡니다.
        //   「실패 0」만 보고 넘기면 안 됩니다 — 검사가 아무 분기도 안 밟았을 수 있습니다.
        Map<SubtractFloor, Map<SubtractVerdict, Integer>> dist = new EnumMap<>(SubtractFloor.class);
        Map<String, Integer> byCondition = new LinkedHashMap<>();

        for (SubtractCondition cond : SubtractCondition.values()) {
            for (SubtractFrequency freq : SubtractFrequency.values()) {

                List<SubtractItem> selected = new ArrayList<>();
                for (Master m : master) {
                    selected.add(new SubtractItem(
                            m.id(), m.name(), m.category(), m.core(), m.base(), m.floor(),
                            "none".equals(m.evidence()), m.freqEditable(),
                            m.freqEditable() ? freq : null, false));
                }

                SubtractPipeline.Outcome o = SubtractPipeline.run(selected, cond, List.of(), null);
                String where = cond.code() + " / " + freq.code();

                // ① 넣은 만큼 나와야 합니다. 하나라도 사라지면 화면에서 항목이 없어집니다
                check("[" + where + "] 결과 수", o.results().size() == master.size(),
                        "넣은 " + master.size() + " 나온 " + o.results().size());

                Set<String> seen = new HashSet<>();
                for (SubtractResult r : o.results()) {
                    judged++;
                    perFloor.merge(r.floor(), 1, Integer::sum);
                    dist.computeIfAbsent(r.floor(), k -> new EnumMap<>(SubtractVerdict.class))
                        .merge(r.verdict(), 1, Integer::sum);
                    if (r.verdict() != SubtractVerdict.KEEP && r.verdict() != SubtractVerdict.EXCLUDED)
                        byCondition.merge(cond.code(), 1, Integer::sum);
                    String tag = "[" + where + "] " + r.itemId();

                    // ② 같은 항목이 두 번 나오면 화면에 중복으로 뜹니다
                    check(tag + " 중복 없음", seen.add(r.itemId()), "두 번 나왔습니다");

                    // ③ ★ 이 검사가 이 파일의 목적입니다 — 하한선 아래로 내려가면 안 됩니다
                    check(tag + " 하한선 지킴", !r.verdict().isBelow(r.floor()),
                            r.floor().code() + " 인데 " + r.verdict().code());

                    // ④ 판정 제외 항목은 어떤 상태에서도 손대지 않습니다 (처방약 · 정기 검진 · 클리닉)
                    if (r.floor() == SubtractFloor.EXCLUDED)
                        check(tag + " 제외 유지", r.verdict() == SubtractVerdict.EXCLUDED,
                                "제외인데 " + r.verdict().code());

                    // ⑤ 근거 문장이 비면 화면에 빈 줄이 뜹니다
                    check(tag + " 근거 있음",
                            r.verdict() == SubtractVerdict.EXCLUDED
                                    || (r.reason() != null && !r.reason().isBlank()),
                            "근거가 비어 있습니다");

                    // ⑥ 되돌리기 버튼을 띄울지가 판정과 어긋나면 안 됩니다
                    check(tag + " 되돌리기 일치",
                            r.verdict().revertible() == (r.verdict() == SubtractVerdict.SIMPLIFY
                                    || r.verdict() == SubtractVerdict.REDUCE
                                    || r.verdict() == SubtractVerdict.SKIP),
                            "revertible 이 판정과 어긋납니다");
                }

                // ⑦ LLM 을 안 넘겼으니 폴백이어야 합니다
                check("[" + where + "] 폴백", !o.llmUsed(), "generator 가 null 인데 llmUsed=true");
            }
        }

        System.out.println("  판정 횟수   " + judged + " (32 x 상태 5 x 빈도 5 = 800)");
        check("판정 800회", judged == 800, "실제 " + judged);

        System.out.println();
        System.out.println("  하한선별 판정 분포 — 어느 분기를 실제로 밟았는가");
        int downgraded = 0;
        for (SubtractFloor f : SubtractFloor.values()) {
            Map<SubtractVerdict, Integer> m = dist.getOrDefault(f, Map.of());
            StringBuilder sb = new StringBuilder();
            for (SubtractVerdict v : SubtractVerdict.values()) {
                Integer n = m.get(v);
                if (n == null) continue;
                sb.append(String.format("%-9s %-4d", v.code(), n));
                if (v != SubtractVerdict.KEEP && v != SubtractVerdict.EXCLUDED) downgraded += n;
            }
            System.out.printf("    %-12s %-5d  %s%n", f.code(), perFloor.getOrDefault(f, 0), sb);
        }
        System.out.println("    덜어낸 판정 " + downgraded + "회 · 상태별 " + byCondition);

        // ★ 이 단정문이 「검사가 아무 일도 안 했다」를 막습니다.
        //   전부 keep 이면 하한선 검사는 통과해도 아무것도 증명하지 못합니다.
        check("덜어내기가 실제로 일어남", downgraded > 0,
                "800회 전부 keep/excluded 입니다 — 이 검사는 아무것도 증명하지 못합니다");
        check("가장 지친 상태에서 덜어냄", byCondition.getOrDefault("drained", 0) > 0,
                "drained 에서조차 아무것도 덜어내지 않았습니다");

        System.out.println();
        System.out.printf("통과 %d · 실패 %d%n", pass, fail);
        for (int i = 0; i < Math.min(failures.size(), 20); i++) System.out.println("  FAIL  " + failures.get(i));
        if (failures.size() > 20) System.out.println("  ... 그 외 " + (failures.size() - 20) + "건");
        if (fail > 0) System.exit(1);
    }
}
