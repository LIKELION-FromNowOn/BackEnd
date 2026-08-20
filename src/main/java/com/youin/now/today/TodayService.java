package com.youin.now.today;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.llm.LlmClient;
import com.youin.now.master.MasterCareItem;
import com.youin.now.master.MasterCareItemRepository;
import com.youin.now.master.MasterCategory;
import com.youin.now.master.MasterCategoryRepository;
import com.youin.now.subtract.VerdictPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 오늘의 행동 {@code NOW-TODAY-001~005}.
 *
 * <p><b>후보 선정과 순위는 코드입니다.</b> LLM 은 1순위 후보 하나를 문장으로 바꾸기만 합니다
 * ({@code docs/prompts/02-today-action.md}).
 *
 * <p><b>{@code actions} 에 마스터 ID 칸이 없습니다.</b> 동결 직전이라 컬럼을 늘리지 않고
 * {@link VerdictPort} 로 매번 {@code userItemId → itemId} 를 다시 찾습니다.
 * {@code userItemId} 로 {@code care_items} 를 찾으면 <b>항상 못 찾습니다</b> —
 * 그쪽은 {@code user_items.id} 입니다.
 *
 * <p>⚠️ <b>{@code RECOMMENDATION_PAUSED} 는 아직 안 넣었습니다.</b> {@code ErrorCode} 에는
 * 있는데 {@code recommendationPaused} 를 읽을 포트가 없습니다.
 */
@Service
public class TodayService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 다시 받기 한도. 초안 3 — 아직 미확정입니다 */
    private static final int REROLL_LIMIT = 3;

    /** 순위식의 「피부·홈케어 항목 가산 3」 */
    private static final String CATEGORY_CARE = "care";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VerdictPort verdicts;
    private final MasterCareItemRepository careItems;
    private final MasterCategoryRepository categories;
    private final TodayActionRepository actions;
    private final LlmClient llm;

    public TodayService(VerdictPort verdicts,
                        MasterCareItemRepository careItems,
                        MasterCategoryRepository categories,
                        TodayActionRepository actions,
                        LlmClient llm) {
        this.verdicts = verdicts;
        this.careItems = careItems;
        this.categories = categories;
        this.actions = actions;
        this.llm = llm;
    }

    // ── NOW-TODAY-001 ────────────────────────────────

    /**
     * 오늘의 행동 조회. <b>없으면 이 시점에 만듭니다.</b>
     *
     * <p><b>판정이 없으면 409 {@code NO_EVALUATION} 입니다.</b> 「후보가 없어서 없다」와
     * 「판정을 아직 안 했다」는 화면에서 다르게 처리해야 합니다.
     *
     * @return 후보가 없으면 {@code null} — 첫 발자국 카드로 넘깁니다
     */
    @Transactional
    public TodayRes.Action getOrCreate(String userId) {
        OffsetDateTime now = OffsetDateTime.now(KST);

        Optional<TodayAction> existing = actions.findByUserIdAndExpiresAtAfter(userId, now);
        if (existing.isPresent()) {
            return toRes(userId, existing.get(), "llm");
        }

        VerdictPort.VerdictSet set = verdicts.of(userId, LocalDate.now(KST))
                .orElseThrow(() -> new ApiException(ErrorCode.NO_EVALUATION));

        return create(userId, set, null, now);
    }

    // ── NOW-TODAY-002 ────────────────────────────────

    /**
     * 다시 받기. <b>직전 추천 항목과 최근 제시 문장은 후보에서 빠집니다.</b>
     *
     * @param actionId 명세가 본문에 요구합니다
     */
    @Transactional
    public TodayRes.Reroll reroll(String userId, String actionId) {
        OffsetDateTime now = OffsetDateTime.now(KST);

        TodayAction action = mine(userId, actionId);

        if (TodayAction.DONE.equals(action.status())) {
            throw new ApiException(ErrorCode.ALREADY_COMPLETED);
        }
        if (action.rerollCount() >= REROLL_LIMIT) {
            throw new ApiException(ErrorCode.REROLL_LIMIT);
        }

        VerdictPort.VerdictSet set = verdicts.of(userId, LocalDate.now(KST))
                .orElseThrow(() -> new ApiException(ErrorCode.NO_EVALUATION));

        short before = action.rerollCount();
        TodayRes.Action a = create(userId, set, action, now);
        if (a == null) {
            throw new ApiException(ErrorCode.ACTION_NOT_FOUND, "제안할 행동이 더 없습니다");
        }
        return new TodayRes.Reroll(
                a.actionId(), a.categoryId(), a.categoryName(), a.title(),
                a.durationSec(), a.rerollLeft(), before + 1,
                a.generatedBy(), a.expiresAt());
    }

    // ── NOW-TODAY-003 ────────────────────────────────

    @Transactional
    public TodayRes.Timer start(String userId, String actionId) {
        TodayAction action = mine(userId, actionId);
        OffsetDateTime now = OffsetDateTime.now(KST);

        action.start(now);
        actions.save(action);

        return new TodayRes.Timer(
                newId("tm_", 20),
                action.id(),
                action.durationSec(),
                TodayRes.iso(now),
                TodayRes.iso(now.plusSeconds(action.durationSec())),
                true);
    }

    // ── NOW-TODAY-004 ────────────────────────────────

    /**
     * 완료. <b>만료 전에도 완료할 수 있습니다.</b>
     *
     * @param timerId 타이머 없이 완료하면 {@code null}
     */
    @Transactional
    public TodayRes.Complete complete(String userId, String actionId, String timerId) {
        TodayAction action = mine(userId, actionId);
        OffsetDateTime now = OffsetDateTime.now(KST);

        MasterCareItem m = masterOf(userId, action);

        action.complete(now);
        actions.save(action);

        return new TodayRes.Complete(
                newId("lg_", 20),
                action.id(),
                TodayRes.iso(now),
                m == null ? null : m.categoryId(),
                timerId != null,
                "오늘 하나 했습니다.");
    }

    // ── NOW-TODAY-005 ────────────────────────────────

    /**
     * 거절 사유 기록. <b>실패로 저장하지 않습니다.</b>
     *
     * <p><b>완료한 행동은 거절할 수 없습니다.</b> 완료 기록이 남아 있는데 상태만
     * 거절로 덮이면 기록 탭에서 어긋납니다.
     */
    @Transactional
    public TodayRes.Reject reject(String userId, String actionId, String reason) {
        if (!List.of("time", "fit", "none").contains(reason)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "거절 사유가 올바르지 않습니다");
        }
        TodayAction action = mine(userId, actionId);

        if (TodayAction.DONE.equals(action.status())) {
            throw new ApiException(ErrorCode.ALREADY_COMPLETED);
        }
        action.reject();
        actions.save(action);

        return new TodayRes.Reject(true, reason);
    }

    // ── 홈 조각 ────────────────────────────────────

    /**
     * 홈의 「오늘의 케어」 조각. <b>{@code docs/04-ports.md} 규약</b> —
     * 각 패키지가 홈에 줄 조각을 스스로 만들고 {@code HomeService} 는 모으기만 합니다.
     *
     * <p><b>홈은 읽기 전용입니다.</b> 여기서 오늘의 행동을 새로 만들지 않습니다 —
     * {@link #getOrCreate} 와 다른 점입니다.
     *
     * @return 오늘 행동이 없으면 {@code null}. 홈은 카드를 안 띄우면 됩니다
     */
    @Transactional(readOnly = true)
    public ForHome todayForHome(String userId) {
        return actions.findByUserIdAndExpiresAtAfter(userId, OffsetDateTime.now(KST))
                .map(a -> new ForHome(
                        a.id(), a.title(), a.durationSec(),
                        a.status(), a.rank(), a.totalCandidates()))
                .orElse(null);
    }

    /** 홈이 그대로 실어 보낼 모양입니다. {@code NOW-HOME-001} 의 {@code today} 블록 */
    public record ForHome(String actionId, String title, int durationSec,
                          String status, short rank, short totalCandidates) { }

    // ── 내부 ────────────────────────────────────────

    /**
     * 후보를 뽑아 1순위를 고르고 문장을 만들어 저장합니다.
     *
     * @param previous 다시 받기면 직전 행동. 그 항목은 후보에서 빠집니다
     * @return 후보가 없으면 {@code null}
     */
    private TodayRes.Action create(String userId, VerdictPort.VerdictSet set,
                                   TodayAction previous, OffsetDateTime now) {

        // ★ LLM 이 실패해도 중복이 안 나가도록 후보 단계에서 거릅니다
        List<String> recent = recentTitles(userId);

        List<Candidate> candidates = new ArrayList<>();
        for (VerdictPort.ItemVerdict v : set.results()) {
            if (!"keep".equals(v.verdict()) && !"simplify".equals(v.verdict())) continue;
            if (previous != null && previous.userItemId().equals(v.userItemId())) continue;

            MasterCareItem master = careItems.findById(v.itemId()).orElse(null);
            if (master == null) continue;                   // 직접 입력 항목은 마스터가 없습니다

            if (recent.contains(fallbackTitle(master))) continue;

            candidates.add(new Candidate(v, master, score(v, master)));
        }
        if (candidates.isEmpty()) return null;              // 첫 발자국 카드로 넘깁니다

        // 동점이면 항목 배열 순서. 무작위 요소가 없습니다
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());
        Candidate picked = candidates.get(0);

        int durationSec = Math.max(60, picked.master().minutes() * 60);

        TodayLlmOut out = askLlm(picked, durationSec, recent);
        String generatedBy = out != null ? "llm" : "fallback";
        String title = out != null ? out.title() : fallbackTitle(picked.master());

        TodayAction action;
        if (previous != null) {
            previous.reroll(title, durationSec, picked.verdict().userItemId(),
                    (short) 1, (short) candidates.size());
            action = previous;
        } else {
            action = new TodayAction(
                    newId("ac_", 26),
                    userId,
                    set.evaluationId(),
                    picked.verdict().userItemId(),
                    title,
                    durationSec,
                    (short) 1,
                    (short) candidates.size(),
                    endOfToday(now));
        }
        actions.save(action);

        return toRes(picked.master(), action, generatedBy);
    }

    /** 폴백 문장. {@code docs/prompts/02-today-action.md} 의 규칙입니다 */
    private static String fallbackTitle(MasterCareItem m) {
        return m.name() + " 한 번만 하기";
    }

    /**
     * 순위식. {@code docs/prompts/02-today-action.md} 기준입니다.
     *
     * <p><b>무작위 요소가 없습니다.</b> 같은 입력에는 항상 같은 결과입니다.
     *
     * <p>⚠️ <b>「오늘 신호 관련도 × 2」와 「최근 3회 제안 감점」은 아직 안 넣었습니다.</b>
     * {@code signals} 와 {@code care_items} 를 잇는 규칙이 명세에 없습니다.
     */
    private int score(VerdictPort.ItemVerdict v, MasterCareItem m) {
        int s = 0;
        s += "keep".equals(v.verdict()) ? 2 : 1;                    // 판정 가중치
        if (CATEGORY_CARE.equals(m.categoryId())) s += 3;           // 피부·홈케어 가산
        return s;
    }

    /** 최근 제시 문장. 후보 필터와 프롬프트 양쪽에 씁니다 */
    private List<String> recentTitles(String userId) {
        return actions.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TodayAction::title)
                .toList();
    }

    /**
     * LLM 호출. <b>실패하면 {@code null} 입니다</b> — 예외를 던지지 않습니다.
     *
     * <p>키가 {@code sk-} 로 시작하지 않으면 부르지도 않고 {@code null} 입니다.
     */
    private TodayLlmOut askLlm(Candidate c, int durationSec, List<String> recent) {
        try {
            String prompt = Files.readString(
                    Path.of("docs/prompts/02-today-action.md"), StandardCharsets.UTF_8);

            ObjectNode payload = MAPPER.createObjectNode();
            ObjectNode picked = payload.putObject("picked");
            picked.put("itemId", c.verdict().itemId());
            picked.put("name", c.master().name());
            picked.put("categoryId", c.master().categoryId());
            picked.put("verdict", c.verdict().verdict());
            picked.put("durationSec", durationSec);

            var titles = payload.putArray("recentTitles");
            recent.forEach(titles::add);

            return llm.ask(prompt, payload.toString(), TodayLlmOut.class);
        } catch (Exception e) {
            return null;                                    // 폴백으로 갑니다
        }
    }

    private TodayAction mine(String userId, String actionId) {
        TodayAction a = actions.findById(actionId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACTION_NOT_FOUND));
        if (!a.userId().equals(userId)) throw new ApiException(ErrorCode.FORBIDDEN);
        return a;
    }

    /**
     * {@code actions} 에 마스터 ID 칸이 없어 판정에서 다시 찾습니다.
     *
     * <p><b>{@code careItems.findById(userItemId)} 는 항상 못 찾습니다.</b>
     * {@code user_items.id} 와 {@code care_items.id} 는 다른 값입니다.
     */
    private MasterCareItem masterOf(String userId, TodayAction a) {
        return verdicts.of(userId, LocalDate.now(KST))
                .flatMap(set -> set.results().stream()
                        .filter(v -> v.userItemId().equals(a.userItemId()))
                        .findFirst())
                .flatMap(v -> careItems.findById(v.itemId()))
                .orElse(null);
    }

    private TodayRes.Action toRes(String userId, TodayAction a, String generatedBy) {
        return toRes(masterOf(userId, a), a, generatedBy);
    }

    private TodayRes.Action toRes(MasterCareItem m, TodayAction a, String generatedBy) {
        String categoryName = m == null ? null
                : categories.findById(m.categoryId()).map(MasterCategory::name).orElse(null);

        return new TodayRes.Action(
                a.id(),
                m == null ? null : m.categoryId(),
                categoryName,
                a.title(),
                a.durationSec(),
                m == null ? null : m.id(),          // ★ 마스터 ID. userItemId 가 아닙니다
                a.status(),
                REROLL_LIMIT - a.rerollCount(),
                generatedBy,
                TodayRes.iso(a.expiresAt()));
    }

    private static String newId(String prefix, int len) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, len);
    }

    /** KST 자정. 프론트 목의 {@code endOfToday} 와 같은 뜻입니다 */
    private static OffsetDateTime endOfToday(OffsetDateTime now) {
        return now.toLocalDate().plusDays(1).atStartOfDay(KST).toOffsetDateTime().minusSeconds(1);
    }

    /** 후보 하나 */
    private record Candidate(VerdictPort.ItemVerdict verdict, MasterCareItem master, int score) { }
}