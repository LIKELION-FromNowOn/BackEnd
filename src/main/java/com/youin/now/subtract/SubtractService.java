package com.youin.now.subtract;

import com.youin.now.checkin.CheckinPort;
import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.item.ItemPort;
import com.youin.now.note.NoteRulePort;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 덜어내기 판정의 ⑧단계 — <b>저장과 조회</b>.
 *
 * <p>①~⑦ 은 {@link SubtractPipeline} 이 합니다. 그 클래스는 DB 를 모르고
 * 순수 계산만 하므로 <b>확인 31건이 DB 없이 돕니다.</b> 그 성질을 깨지 않으려고
 * 저장을 여기로 뺐습니다.
 *
 * <p><b>2026-08-20 현재 LLM 을 붙이지 않았습니다.</b> {@code generator} 에 {@code null} 을
 * 넘기면 파이프라인이 폴백 문장을 씁니다. {@code generatedBy} 가 {@code fallback} 으로 남고,
 * 발표에서 그 필드로 「AI 가 실제로 돌았는지」를 구분합니다.
 */
@Service
public class SubtractService {

    /**
     * 판정에 필요한 최소 항목 수. <b>명세서 {@code NOW-SUB-001} 이 3개로 정했습니다.</b>
     *
     * <p>{@code schema_v63.sql:232} 은 「미확정(제안값 3)」이라 DB 제약으로 걸지 않고
     * 애플리케이션에서 봅니다. 값이 바뀌면 여기 한 줄만 고치면 됩니다.
     */
    private static final int MIN_ITEMS = 3;

    /** 명세서가 응답에 쓰는 ISO 8601. {@code 2026-08-20T09:12:00+09:00} 모양입니다. */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final CheckinPort checkins;
    private final ItemPort items;
    private final NoteRulePort noteRules;
    private final EvaluationRepository evaluations;
    private final EvaluationResultRepository results;

    public SubtractService(CheckinPort checkins, ItemPort items, NoteRulePort noteRules,
                           EvaluationRepository evaluations, EvaluationResultRepository results) {
        this.checkins = checkins;
        this.items = items;
        this.noteRules = noteRules;
        this.evaluations = evaluations;
        this.results = results;
    }

    /**
     * {@code NOW-SUB-001} 판정 실행.
     *
     * <p><b>상태 체크가 없으면 409 {@code NO_CHECKIN} 입니다.</b> 명세서가 「판정 전 필수」로
     * 정해 두었습니다. 순서를 강제하는 것이 이 앱의 하루 사이클입니다.
     */
    @Transactional
    public SubtractRes evaluate(String userId, String checkinId) {

        CheckinPort.LatestCheckin checkin = checkins.latest(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NO_CHECKIN));

        SubtractCondition condition = SubtractCondition.of(checkin.state());

        // ① 사용자가 고른 항목. ItemPort 가 아직 스텁이라 빈 목록이 옵니다 — 정상입니다
        List<ItemPort.SelectedItem> picked = items.selected(userId);

        // 명세서 NOW-SUB-001 — 3개 미만이면 400 입니다.
        // 2026-08-20 이전에는 이 검사가 없어서 빈 결과를 200 으로 내보냈습니다.
        // 명세서에 「어떤 경우에도 빈 결과를 반환하지 않습니다」가 있는데 정반대였습니다.
        if (picked.size() < MIN_ITEMS) {
            throw new ApiException(ErrorCode.MIN_ITEMS_REQUIRED);
        }

        // itemId -> 마스터 정보. 응답에 name · evidenceLevel · frequency 를 실을 때 씁니다
        Map<String, ItemPort.SelectedItem> master = new LinkedHashMap<>();
        List<SubtractItem> selected = new ArrayList<>();
        for (ItemPort.SelectedItem s : picked) {
            master.put(s.itemId(), s);
            selected.add(new SubtractItem(
                    s.itemId(), s.name(), s.categoryId(),
                    // 2026-08-20 까지 여기에 0 이 박혀 있었습니다. 점수식이 core 와 base 를
                    // 쓰는데 둘 다 0 이면 모든 항목의 점수가 같아져 판정이 전부 동일해집니다.
                    // 스텁이 빈 목록이라 드러나지 않았을 뿐입니다
                    s.core(), s.base(),
                    SubtractFloor.ofLevel(s.floor()),
                    // ItemPort 는 근거 등급을 숫자로 넘깁니다. 0 이면 「근거 없음」이고
                    // ④단계에서 keep 으로 고정됩니다 — LLM 이 손대지 못하는 자리입니다
                    s.evidenceLevel() == 0,
                    s.frequency() != null,
                    SubtractFrequency.ofOrNull(s.frequency()),
                    false));
        }

        // ③ 오늘 살아 있는 클리닉 제한
        List<ClinicCaution> cautions = new ArrayList<>();
        for (NoteRulePort.NoteRule r : noteRules.activeRules(userId)) {
            if (r.itemId() != null) {
                cautions.add(new ClinicCaution(r.itemId(), r.sentenceNo(), r.daysPeriod(), r.daysLeft()));
            }
        }

        // ②④⑤⑥⑦ — generator 가 null 이면 전부 폴백 문장입니다
        SubtractPipeline.Outcome outcome =
                SubtractPipeline.run(selected, condition, cautions, null);

        String generatedBy = outcome.llmUsed() ? "llm" : "fallback";

        Evaluation ev = evaluations.findByCheckinId(checkinId)
                .map(e -> { e.update(condition.code(), condition.judgeStrength(), generatedBy); return e; })
                .orElseGet(() -> new Evaluation(Ids.evaluation(), userId, checkinId,
                        condition.code(), condition.judgeStrength(), generatedBy));
        evaluations.save(ev);

        // 다시 판정하면 결과도 새로 씁니다
        results.deleteByEvaluationId(ev.id());
        List<EvaluationResult> rows = new ArrayList<>();
        for (SubtractResult r : outcome.results()) {
            ItemPort.SelectedItem m = master.get(r.itemId());
            rows.add(new EvaluationResult(
                    // ★ 외래키가 user_items(id) 를 봅니다. 마스터 ID 를 넣으면 저장이 터집니다
                    Ids.result(), ev.id(), m.userItemId(),
                    r.verdict().code(), r.reason(),
                    // REQUESTS #24 해소 — 더 이상 medium 으로 채우지 않습니다
                    evidenceCode(m.evidenceLevel()),
                    r.floor().code(), r.floorApplied(),
                    r.excludedBy(),
                    r.noteSent() == null ? null : r.noteSent().shortValue(),
                    r.daysLeft() == null ? null : r.daysLeft().shortValue()));
        }
        results.saveAll(rows);

        Map<String, ItemPort.SelectedItem> byUi = new LinkedHashMap<>();
        for (ItemPort.SelectedItem m2 : picked) byUi.put(m2.userItemId(), m2);
        return toRes(ev, rows, byUi, List.of());
    }

    /**
     * {@code NOW-SUB-002} 판정 결과 조회.
     *
     * <p><b>재판정하지 않고 같은 결과를 다시 봅니다.</b> 앱을 껐다 켜도 오늘의 판정이
     * 유지되어야 해서 있는 API 입니다.
     *
     * @param evaluationId 없으면 <b>가장 최근 판정</b>입니다
     * @param verdict      {@code reduce,skip} 처럼 콤마로 여러 개. 없으면 전부.
     *                     <b>모르는 값이 섞이면 400</b> 입니다 — 조용히 무시하면 프론트가
     *                     오타를 눈치채지 못하고 빈 목록을 사용자에게 보여 줍니다
     */
    @Transactional(readOnly = true)
    public SubtractRes result(String userId, String evaluationId, String verdict) {
        // ★ 조회보다 먼저 봅니다. 판정이 없을 때 오타를 404 로 덮으면
        //   프론트가 오타를 눈치채지 못하고 「판정이 없구나」로 읽습니다
        List<String> filter = parseVerdicts(verdict);

        Evaluation ev = (evaluationId == null || evaluationId.isBlank())
                ? evaluations.findTopByUserIdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() -> new ApiException(ErrorCode.EVALUATION_NOT_FOUND))
                : evaluations.findById(evaluationId)
                        .orElseThrow(() -> new ApiException(ErrorCode.EVALUATION_NOT_FOUND));

        // 남의 판정을 ID 로 찍어 보는 것을 막습니다
        if (!ev.userId().equals(userId)) throw new ApiException(ErrorCode.FORBIDDEN);

        return toRes(ev, results.findByEvaluationId(ev.id()), byUserItemId(userId), filter);
    }

    private List<String> parseVerdicts(String verdict) {
        if (verdict == null || verdict.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String raw : verdict.split(",")) {
            String v = raw.trim();
            if (v.isEmpty()) continue;
            // 모르는 값은 400 입니다. 그냥 두면 IllegalArgumentException 이 새 나가 500 이 됩니다 —
            // 프론트가 자기 오타를 「서버 장애」로 읽게 됩니다
            try {
                SubtractVerdict.of(v);
            } catch (IllegalArgumentException e) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "판정 값이 올바르지 않습니다");
            }
            if (!out.contains(v)) out.add(v);
        }
        return out;
    }

    /**
     * {@code NOW-SUB-003} 판정 되돌리기.
     *
     * <p><b>{@code excluded} 는 되돌릴 수 없습니다.</b> 클리닉 안내는 앱이 판단하지 않는
     * 영역이라, 되돌리기를 허용하면 앱이 그 안내를 덮어쓰는 셈이 됩니다.
     * 이 앱의 차별점이 여기 걸려 있습니다.
     */
    @Transactional
    public SubtractRevertRes revert(String userId, String itemId, String evaluationId) {
        Evaluation ev = evaluations.findById(evaluationId)
                .orElseThrow(() -> new ApiException(ErrorCode.EVALUATION_NOT_FOUND));
        if (!ev.userId().equals(userId)) throw new ApiException(ErrorCode.FORBIDDEN);

        // 화면은 마스터 ID(cr4)를 보내는데 저장은 user_items.id 로 되어 있어 한 번 옮깁니다
        ItemPort.SelectedItem sel = items.selected(userId).stream()
                .filter(x -> x.itemId().equals(itemId) || x.userItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.ITEM_NOT_FOUND));

        EvaluationResult r = results.findByEvaluationIdAndUserItemId(ev.id(), sel.userItemId())
                .orElseThrow(() -> new ApiException(ErrorCode.ITEM_NOT_FOUND));

        if (SubtractVerdict.EXCLUDED.code().equals(r.verdict())) {
            throw new ApiException(ErrorCode.CANNOT_REVERT_EXCLUDED);
        }
        if (r.reverted()) {
            throw new ApiException(ErrorCode.ALREADY_REVERTED);
        }
        r.revert();
        results.save(r);

        // persisted 는 항상 true 입니다. 되돌리기는 한 번으로 끝나고 다음 판정에서도
        // keep 으로 고정됩니다 — 사용자가 같은 항목을 매번 되돌릴 일이 없습니다
        return new SubtractRevertRes(itemId, SubtractVerdict.KEEP.code(), true,
                summarize(results.findByEvaluationId(ev.id())));
    }

    /**
     * 근거 등급 숫자를 명세서의 문자열로. {@code care_items.evidence_level} 과 같은 값입니다.
     *
     * <p>{@code ItemPort} 가 숫자로 넘기는 것은 {@code docs/04-ports.md} 규약이라 그대로 뒀고,
     * 변환만 여기서 합니다. <b>낮을수록 확실합니다</b> — 프로토타입의 {@code EVI} 와 같습니다.
     */
    private static String evidenceCode(int level) {
        return switch (level) {
            case 1 -> "high";
            case 2 -> "medium";
            case 3 -> "low";
            default -> "none";
        };
    }

    /**
     * 저장된 판정에 <b>이름과 빈도를 다시 붙이기 위한</b> 조회.
     *
     * <p>{@code evaluation_results} 는 {@code user_item_id} 만 갖고 있어서
     * {@code name} 과 {@code frequency} 를 그것만으로는 못 만듭니다.
     * <b>스키마를 늘리는 대신 {@code ItemPort} 를 한 번 더 부릅니다</b> —
     * 사용자의 선택 목록은 판정과 조회 사이에 거의 바뀌지 않고, 남의 테이블을 직접 읽지 않아도 됩니다.
     *
     * <p><b>판정 뒤에 항목을 지우면</b> 그 항목만 이름이 빠집니다. 판정 자체는 그대로 남습니다.
     */
    private Map<String, ItemPort.SelectedItem> byUserItemId(String userId) {
        Map<String, ItemPort.SelectedItem> m = new LinkedHashMap<>();
        for (ItemPort.SelectedItem s : items.selected(userId)) m.put(s.userItemId(), s);
        return m;
    }

    private SubtractRes.Summary summarize(List<EvaluationResult> rows) {
        int keep = 0, simplify = 0, reduce = 0, skip = 0, excluded = 0;
        for (EvaluationResult r : rows) {
            switch (r.verdict()) {
                case "keep"     -> keep++;
                case "simplify" -> simplify++;
                case "reduce"   -> reduce++;
                case "skip"     -> skip++;
                case "excluded" -> excluded++;
                default -> { }
            }
        }
        return new SubtractRes.Summary(keep, simplify, reduce, skip, excluded);
    }

    /**
     * @param filter 적용된 {@code verdict} 필터. 비어 있으면 전부 내려갑니다.
     *               <b>{@code summary} 는 필터와 무관하게 전체 기준</b>입니다 — 명세서가 그렇게 정했습니다
     */
    private SubtractRes toRes(Evaluation ev, List<EvaluationResult> rows,
                              Map<String, ItemPort.SelectedItem> master, List<String> filter) {

        SubtractRes.Summary summary = summarize(rows);   // ★ 필터 전에 셉니다

        List<SubtractRes.Item> out = new ArrayList<>();
        for (EvaluationResult r : rows) {
            if (!filter.isEmpty() && !filter.contains(r.verdict())) continue;

            ItemPort.SelectedItem m = master.get(r.userItemId());
            boolean isExcluded = "excluded".equals(r.verdict());
            out.add(new SubtractRes.Item(
                    m == null ? r.userItemId() : m.itemId(),
                    m == null ? null : m.name(),
                    m == null ? null : m.frequency(),
                    r.verdict(), r.reason(),
                    r.evidenceLevel(), r.floor(),
                    r.floorApplied(), r.reverted(),
                    !isExcluded && !r.reverted(),        // excluded 는 되돌리기 버튼을 띄우지 않습니다
                    r.excludedBy(),
                    r.noteSent() == null ? null : r.noteSent().intValue(),
                    r.daysLeft() == null ? null : r.daysLeft().intValue()));
        }
        return new SubtractRes(
                ev.id(), ev.checkinId(),
                ev.createdAt() == null ? null : ISO.format(ev.createdAt()),
                ev.state(), ev.judgeStrength(), filter, ev.generatedBy(),
                summary, out);
    }
}
