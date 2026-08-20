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
import java.util.UUID;

/**
 * 오늘의 행동 {@code NOW-TODAY-001~005}.
 *
 * <p><b>후보 선정과 순위는 코드입니다.</b> LLM 은 1순위 후보 하나를 문장으로 바꾸기만 합니다
 * ({@code docs/prompts/02-today-action.md}).
 *
 * <p><b>{@code durationSec} 도 서버가 정합니다.</b> 마스터의 {@code minutes} 를 씁니다.
 *
 * <p><b>후보가 없으면 {@code null} 입니다.</b> 예외가 아닙니다 — 첫 발자국 카드로 넘어갈 자리입니다.
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
     * @return 후보가 없으면 {@code null} — 첫 발자국 카드로 넘깁니다
     */
    @Transactional
    public TodayRes.Action getOrCreate(String userId) {
        OffsetDateTime now = OffsetDateTime.now(KST);

        return actions.findByUserIdAndExpiresAtAfter(userId, now)
                .map(a -> toRes(a, "llm"))
                .orElseGet(() -> create(userId, null, now));
    }

    // ── NOW-TODAY-002 ────────────────────────────────

    /** 다시 받기. <b>직전 추천 항목은 후보에서 빠집니다</b> */
    @Transactional
    public TodayRes.Action reroll(String userId) {
        OffsetDateTime now = OffsetDateTime.now(KST);

        TodayAction action = actions.findByUserIdAndExpiresAtAfter(userId, now)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "오늘의 행동이 없습니다"));

        if (action.rerollCount() >= REROLL_LIMIT) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "다시 받기 한도를 넘었습니다");
        }
        return create(userId, action, now);
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

        action.complete(now);
        actions.save(action);

        String categoryId = careItems.findById(action.userItemId())
                .map(MasterCareItem::categoryId)
                .orElse(null);

        return new TodayRes.Complete(
                newId("lg_", 20),
                action.id(),
                TodayRes.iso(now),
                categoryId,
                timerId != null,
                "오늘 하나 했습니다.");
    }

    // ── NOW-TODAY-005 ────────────────────────────────

    /**
     * 거절 사유 기록. <b>실패로 저장하지 않습니다.</b>
     *
     * <table>
     *   <tr><td>{@code time}</td><td>더 짧은 것으로 다시 고름</td></tr>
     *   <tr><td>{@code fit}</td><td>오늘 후보에서 제외</td></tr>
     *   <tr><td>{@code none}</td><td>제안 중단. 첫 발자국 카드도 내림</td></tr>
     * </table>
     */
    @Transactional
    public TodayRes.Reject reject(String userId, String actionId, String reason) {
        if (!List.of("time", "fit", "none").contains(reason)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "거절 사유가 올바르지 않습니다");
        }
        TodayAction action = mine(userId, actionId);
        action.reject();
        actions.save(action);

        return new TodayRes.Reject(true, reason);
    }

    // ── 내부 ────────────────────────────────────────

    /**
     * 후보를 뽑아 1순위를 고르고 문장을 만들어 저장합니다.
     *
     * @param previous 다시 받기면 직전 행동. 그 항목은 후보에서 빠집니다
     * @return 판정이 없거나 후보가 없으면 {@code null}
     */
    private TodayRes.Action create(String userId, TodayAction previous, OffsetDateTime now) {

        VerdictPort.VerdictSet set = verdicts.of(userId, LocalDate.now(KST)).orElse(null);
        if (set == null) return null;                       // 그날 판정이 아직 없습니다

        // 후보 — keep 또는 simplify 이고 직전 추천이 아닌 것
        List<Candidate> candidates = new ArrayList<>();
        for (VerdictPort.ItemVerdict v : set.results()) {
            if (!"keep".equals(v.verdict()) && !"simplify".equals(v.verdict())) continue;
            if (previous != null && previous.userItemId().equals(v.userItemId())) continue;

            MasterCareItem master = careItems.findById(v.itemId()).orElse(null);
            if (master == null) continue;                   // 직접 입력 항목은 마스터가 없습니다

            candidates.add(new Candidate(v, master, score(v, master)));
        }
        if (candidates.isEmpty()) return null;              // 첫 발자국 카드로 넘깁니다

        // 동점이면 항목 배열 순서. 무작위 요소가 없습니다
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());
        Candidate picked = candidates.get(0);

        int durationSec = Math.max(60, picked.master().minutes() * 60);

        TodayLlmOut out = askLlm(picked, durationSec, recentTitles(userId));
        String generatedBy = out != null ? "llm" : "fallback";
        String title = out != null
                ? out.title()
                : picked.master().name() + " 한 번만 하기";     // 폴백 — 프롬프트 문서의 문장

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
        return toRes(action, generatedBy);
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

    /** 최근 제시 문장. 프롬프트가 {@code recentTitles} 로 중복 금지에 씁니다 */
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
     * 그래도 앱은 정상이고 폴백 문장으로 갑니다.
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
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "행동을 찾을 수 없습니다"));
        if (!a.userId().equals(userId)) throw new ApiException(ErrorCode.FORBIDDEN);
        return a;
    }

    private TodayRes.Action toRes(TodayAction a, String generatedBy) {
        MasterCareItem m = careItems.findById(a.userItemId()).orElse(null);
        String categoryName = m == null ? null
                : categories.findById(m.categoryId()).map(MasterCategory::name).orElse(null);

        return new TodayRes.Action(
                a.id(),
                m == null ? null : m.categoryId(),
                categoryName,
                a.title(),
                a.durationSec(),
                a.userItemId(),
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