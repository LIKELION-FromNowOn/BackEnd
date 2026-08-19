package com.youin.now.subtract;

import com.youin.now.checkin.CheckinPort;
import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.item.ItemPort;
import com.youin.now.note.NoteRulePort;
import java.util.ArrayList;
import java.util.List;
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
        List<SubtractItem> selected = new ArrayList<>();
        for (ItemPort.SelectedItem s : items.selected(userId)) {
            selected.add(new SubtractItem(
                    s.itemId(), s.itemId(), null,
                    0, 0,
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
            rows.add(new EvaluationResult(
                    Ids.result(), ev.id(), r.itemId(),
                    r.verdict().code(), r.reason(),
                    // 근거 등급은 마스터에서 오는데 ItemPort 가 아직 등급을 안 넘겨줍니다.
                    // 스키마가 NOT NULL 이라 판정에 영향이 없는 medium 으로 채웁니다 (REQUESTS #24)
                    "medium",
                    r.floor().code(), r.floorApplied(),
                    r.excludedBy(),
                    r.noteSent() == null ? null : r.noteSent().shortValue(),
                    r.daysLeft() == null ? null : r.daysLeft().shortValue()));
        }
        results.saveAll(rows);

        return toRes(ev, checkin, rows);
    }

    /** {@code NOW-SUB-002} 최근 판정 조회. */
    @Transactional(readOnly = true)
    public SubtractRes latest(String userId) {
        Evaluation ev = evaluations.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.EVALUATION_NOT_FOUND));
        return toRes(ev, null, results.findByEvaluationId(ev.id()));
    }

    /**
     * {@code NOW-SUB-003} 판정 되돌리기.
     *
     * <p><b>{@code excluded} 는 되돌릴 수 없습니다.</b> 클리닉 안내는 앱이 판단하지 않는
     * 영역이라, 되돌리기를 허용하면 앱이 그 안내를 덮어쓰는 셈이 됩니다.
     * 이 앱의 차별점이 여기 걸려 있습니다.
     */
    @Transactional
    public SubtractRes revert(String userId, String itemId) {
        Evaluation ev = evaluations.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.EVALUATION_NOT_FOUND));

        EvaluationResult r = results.findByEvaluationIdAndUserItemId(ev.id(), itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.ITEM_NOT_FOUND));

        if (SubtractVerdict.EXCLUDED.code().equals(r.verdict())) {
            throw new ApiException(ErrorCode.CANNOT_REVERT_EXCLUDED);
        }
        if (r.reverted()) {
            throw new ApiException(ErrorCode.ALREADY_REVERTED);
        }
        r.revert();
        results.save(r);

        return toRes(ev, null, results.findByEvaluationId(ev.id()));
    }

    private SubtractRes toRes(Evaluation ev, CheckinPort.LatestCheckin checkin,
                              List<EvaluationResult> rows) {
        int keep = 0, simplify = 0, reduce = 0, skip = 0, excluded = 0;
        List<SubtractRes.Item> items = new ArrayList<>();
        for (EvaluationResult r : rows) {
            switch (r.verdict()) {
                case "keep"     -> keep++;
                case "simplify" -> simplify++;
                case "reduce"   -> reduce++;
                case "skip"     -> skip++;
                case "excluded" -> excluded++;
                default -> { }
            }
            boolean isExcluded = "excluded".equals(r.verdict());
            items.add(new SubtractRes.Item(
                    r.userItemId(), r.verdict(), r.reason(),
                    r.floor(), r.floorApplied(), r.reverted(),
                    !isExcluded && !r.reverted(),        // excluded 는 되돌리기 버튼을 띄우지 않습니다
                    r.excludedBy(),
                    r.noteSent() == null ? null : r.noteSent().intValue(),
                    r.daysLeft() == null ? null : r.daysLeft().intValue()));
        }
        return new SubtractRes(
                ev.id(), ev.checkinId(), ev.state(), ev.judgeStrength(), ev.generatedBy(),
                new SubtractRes.Summary(keep, simplify, reduce, skip, excluded),
                items);
    }
}
