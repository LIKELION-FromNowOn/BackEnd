package com.youin.now.care;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.note.NoteRulePort;
import com.youin.now.safety.SafetyPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 예정 {@code NOW-NOTE-004 · 005 · 006}.
 *
 * <p>{@code GET} 과 {@code PUT /me/care} 는 송원석 님 몫입니다 —
 * {@code care_notes} 삼총사를 다 읽고 써야 해서 그쪽이 맞습니다.
 *
 * <p><b>{@code title} 은 자유 입력이라 저장 전에 {@link SafetyPort} 를 통과합니다.</b>
 * {@code Source} 는 {@code PLAN} 입니다 — {@code NOTE} 가 아닙니다.
 *
 * <p><b>{@code plans} 는 완전히 독립된 테이블입니다.</b> 안내문 규칙과는
 * {@link NoteRulePort} 를 통해서만 만납니다.
 */
@Service
public class PlanService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PlanRepository plans;
    private final NoteRulePort noteRules;
    private final SafetyPort safety;

    public PlanService(PlanRepository plans, NoteRulePort noteRules, SafetyPort safety) {
        this.plans = plans;
        this.noteRules = noteRules;
        this.safety = safety;
    }

    // ── NOW-NOTE-004 ────────────────────────────────

    /** 예정 목록. 각 예정이 안내문 규칙에 걸리는지 같이 내려줍니다 */
    @Transactional(readOnly = true)
    public PlanRes.Plans getPlans(String userId) {
        List<NoteRulePort.NoteRule> rules = noteRules.activeRules(userId);

        List<PlanRes.Item> out = new ArrayList<>();
        for (Plan p : plans.findByUserIdOrderByPlanDateAscCreatedAtAsc(userId)) {
            Conflict c = conflictOf(p.title(), rules);
            out.add(new PlanRes.Item(
                    p.id(),
                    p.planDate().toString(),
                    p.title(),
                    c.hit(),
                    c.freeFrom(),
                    c.sent()));
        }
        return new PlanRes.Plans(out);
    }

    // ── NOW-NOTE-005 ────────────────────────────────

    /** 예정 추가. <b>캘린더 연동이 아닙니다.</b> 저장 전에 위기 신호 검사를 통과해야 합니다 */
    @Transactional
    public PlanRes.Created addPlan(String userId, PlanReq.Add req) {

        SafetyPort.SafetyResult r = safety.check(req.title(), SafetyPort.Source.PLAN);
        if (r.blocked()) {
            throw new ApiException(ErrorCode.TEXT_REJECTED, r.message());
        }

        LocalDate date = parseDate(req.date());

        Plan p = new Plan(Ids.of("pl"), userId, req.title(), date);
        plans.save(p);

        Conflict c = conflictOf(p.title(), noteRules.activeRules(userId));

        return new PlanRes.Created(
                p.id(), p.planDate().toString(), p.title(), c.hit(), c.freeFrom());
    }

    // ── NOW-NOTE-006 ────────────────────────────────

    /**
     * 예정 삭제. <b>없는 id 도 성공입니다(멱등).</b> 프론트 목 주석의 규칙입니다.
     *
     * <p><b>소유자를 반드시 확인합니다.</b> {@code findById} 로 지우면 남의 예정이 지워집니다.
     */
    @Transactional
    public void deletePlan(String userId, String planId) {
        plans.findByIdAndUserId(planId, userId).ifPresent(plans::delete);
    }

    // ── 내부 ────────────────────────────────────────

    /**
     * 예정 제목이 안내문 규칙에 걸리는지.
     *
     * <p><b>여러 규칙에 걸리면 가장 늦게 풀리는 것</b>을 돌려줍니다 —
     * 먼저 풀리는 것을 보여 주면 틀린 안내가 됩니다.
     */
    private Conflict conflictOf(String title, List<NoteRulePort.NoteRule> rules) {
        LocalDate today = LocalDate.now(KST);

        NoteRulePort.NoteRule worst = null;
        for (NoteRulePort.NoteRule r : rules) {
            if (r.daysLeft() <= 0) continue;
            if (r.keywords() == null) continue;
            if (r.keywords().stream().noneMatch(title::contains)) continue;

            if (worst == null || r.daysLeft() > worst.daysLeft()) worst = r;
        }
        if (worst == null) return new Conflict(false, null, null);

        return new Conflict(true,
                today.plusDays(worst.daysLeft()).toString(),
                worst.sentenceNo());
    }

    /** 날짜가 깨져 들어오면 500 대신 400 입니다 */
    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "날짜 형식이 올바르지 않습니다");
        }
    }

    private record Conflict(boolean hit, String freeFrom, Integer sent) { }
}