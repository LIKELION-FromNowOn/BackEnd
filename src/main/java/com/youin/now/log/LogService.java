package com.youin.now.log;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.item.ItemPort;
import com.youin.now.master.MasterCareItem;
import com.youin.now.master.MasterCareItemRepository;
import com.youin.now.master.MasterCategory;
import com.youin.now.master.MasterCategoryRepository;
import com.youin.now.subtract.VerdictPort;
import com.youin.now.today.TodayAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기록 {@code NOW-LOG-001 · 002}.
 *
 * <p><b>{@code actions} 파생입니다.</b> {@code daily_logs} 는 쓰지 않습니다
 * ({@code .agent/REQUESTS.md} #2 확정). 그래서 「그날 첫 행을 누가 만드나」 규약이 필요 없습니다.
 *
 * <p><b>완료한 것만 남깁니다.</b> 달성률·연속일은 만들지 않습니다 —
 * 비율을 만들면 못 한 날이 드러나고, 끊기는 순간이 부담이 되기 때문입니다.
 *
 * <p>⚠️ <b>{@code daysRecorded} 와 {@code topState} 가 아직 0 · null 입니다.</b>
 * {@code CheckinPort} 대기입니다.
 */
@Service
@Transactional(readOnly = true)
public class LogService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** {@code week} 7일 · {@code month} 30일. 홈의 {@code unlock.monthlyNeed} 가 30 입니다 */
    private static final int WEEK_DAYS = 7;
    private static final int MONTH_DAYS = 30;

    /** {@code limit} 기본 30 · 최대 100 */
    private static final int LIMIT_DEFAULT = 30;
    private static final int LIMIT_MAX = 100;

    private final LogActionRepository actions;
    private final ItemPort items;
    private final MasterCategoryRepository categories;
    private final MasterCareItemRepository careItems;
    private final VerdictPort verdicts;

    public LogService(LogActionRepository actions,
                      ItemPort items,
                      MasterCategoryRepository categories,
                      MasterCareItemRepository careItems,
                      VerdictPort verdicts) {
        this.actions = actions;
        this.items = items;
        this.categories = categories;
        this.careItems = careItems;
        this.verdicts = verdicts;
    }

    // ── NOW-LOG-001 ────────────────────────────────

    /**
     * 완료한 행동. <b>평평한 배열입니다</b> — 날짜별로 묶는 것은 화면이 합니다.
     *
     * @param from       없으면 30일 전
     * @param to         없으면 오늘
     * @param categoryId 없으면 전부. <b>없는 분류면 400</b>
     * @param limit      1~100, 기본 30
     */
    public LogRes.Logs getLogs(String userId, String from, String to,
                               String categoryId, Integer limit) {

        LocalDate toDate = (to == null || to.isBlank())
                ? LocalDate.now(KST) : parseDate(to);
        LocalDate fromDate = (from == null || from.isBlank())
                ? toDate.minusDays(MONTH_DAYS) : parseDate(from);

        if (fromDate.isAfter(toDate)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "시작일이 종료일보다 늦습니다");
        }

        int max = (limit == null) ? LIMIT_DEFAULT : limit;
        if (max < 1 || max > LIMIT_MAX) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "limit 은 1에서 100 사이입니다");
        }

        Map<String, String> names = categoryNames();

        if (categoryId != null && !categoryId.isBlank() && !names.containsKey(categoryId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 카테고리입니다");
        }

        Map<String, String> categoryOf = categoryIdByUserItemId(userId);

        List<LogRes.Item> all = new ArrayList<>();
        for (TodayAction a : done(userId, fromDate, toDate)) {
            String cid = categoryOf.get(a.userItemId());

            // 분류 필터. 분류를 못 찾은 것은 필터가 걸려 있으면 뺍니다
            if (categoryId != null && !categoryId.isBlank() && !categoryId.equals(cid)) continue;

            all.add(new LogRes.Item(
                    a.id(),
                    a.completedAt().atZoneSameInstant(KST).toLocalDate().toString(),
                    cid,
                    cid == null ? null : names.get(cid),
                    a.title(),
                    a.startedAt() != null));
        }

        // total 은 자르기 전 기준입니다
        int total = all.size();
        List<LogRes.Item> logs = all.size() > max ? List.copyOf(all.subList(0, max)) : all;

        return new LogRes.Logs(logs, total, total > max);
    }

    // ── NOW-LOG-002 ────────────────────────────────

    /**
     * 건수와 분포만. <b>분모를 계산하지 않습니다.</b>
     *
     * @param period {@code week} 또는 {@code month}. 없으면 {@code month}
     */
    public LogRes.Summary getSummary(String userId, String period) {
        String p = (period == null || period.isBlank()) ? "month" : period;
        if (!List.of("week", "month").contains(p)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "period 값이 올바르지 않습니다");
        }

        LocalDate toDate = LocalDate.now(KST);
        LocalDate fromDate = toDate.minusDays("week".equals(p) ? WEEK_DAYS : MONTH_DAYS);

        Map<String, String> names = categoryNames();
        Map<String, String> categoryOf = categoryIdByUserItemId(userId);

        List<TodayAction> rows = done(userId, fromDate, toDate);

        // 분류별 건수. 순서는 처음 나온 순입니다
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TodayAction a : rows) {
            String cid = categoryOf.get(a.userItemId());
            if (cid == null || !names.containsKey(cid)) continue;
            counts.merge(cid, 1, Integer::sum);
        }

        List<LogRes.CategoryCount> byCategory = new ArrayList<>();
        counts.forEach((cid, c) ->
                byCategory.add(new LogRes.CategoryCount(cid, names.get(cid), c)));

        // 덜어내기 통계는 창구로 받습니다. 이름은 안 담겨 오니 여기서 붙입니다
        VerdictPort.Stats st = verdicts.stats(userId, fromDate, toDate);

        List<LogRes.ItemCount> topSubtracted = st.topSubtracted().stream()
                .map(x -> new LogRes.ItemCount(
                        x.itemId(),
                        careItems.findById(x.itemId()).map(MasterCareItem::name).orElse(null),
                        x.count()))
                .toList();

        // TODO CheckinPort 가 열리면 채웁니다 (이철희 님)
        //      daysRecorded = COUNT(DISTINCT check_date) FROM checkins
        //      topState     = checkins.state 최빈값
        //      홈의 unlock.recordedDays 와 같은 값입니다
        int daysRecorded = 0;
        String topState = null;

        return new LogRes.Summary(
                p, rows.size(), byCategory, sentenceOf(p, byCategory),
                daysRecorded, st.daysSubtracted(), topState, topSubtracted);
    }

    // ── 내부 ────────────────────────────────────────

    /**
     * 요약 문장. <b>가장 많이 한 카테고리 하나로 만듭니다.</b>
     *
     * <p><b>한 것만 가지고 만듭니다.</b> 「운동이 부족합니다」처럼 부족한 카테고리를
     * 지적하지 않습니다 — 명세의 단서입니다. 못 한 것을 드러내면 부담이 됩니다.
     *
     * @return 완료한 것이 없으면 {@code null}. 화면이 문장을 안 띄우면 됩니다
     */
    private String sentenceOf(String period, List<LogRes.CategoryCount> byCategory) {
        if (byCategory.isEmpty()) return null;

        LogRes.CategoryCount top = byCategory.get(0);
        for (LogRes.CategoryCount c : byCategory) {
            if (c.count() > top.count()) top = c;
        }

        String when = "week".equals(period) ? "이번 주" : "이번 달";
        return when + "에는 " + top.categoryName()
                + objectParticle(top.categoryName()) + " 가장 많이 챙겼습니다.";
    }

    /** 받침이 있으면 「을」, 없으면 「를」. 「수면을」 「피부 · 홈케어를」 */
    private static String objectParticle(String word) {
        if (word == null || word.isEmpty()) return "를";
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) return "를";
        return (last - 0xAC00) % 28 == 0 ? "를" : "을";
    }

    /** 그 기간에 완료한 행동. <b>{@code done} 상태만</b> 셉니다 */
    private List<TodayAction> done(String userId, LocalDate from, LocalDate to) {
        OffsetDateTime start = from.atStartOfDay(KST).toOffsetDateTime();
        OffsetDateTime end = to.plusDays(1).atStartOfDay(KST).toOffsetDateTime().minusNanos(1);

        return actions.findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
                userId, TodayAction.DONE, start, end);
    }

    private Map<String, String> categoryNames() {
        Map<String, String> m = new LinkedHashMap<>();
        for (MasterCategory c : categories.findAllByOrderBySortOrderAsc()) {
            m.put(c.id(), c.name());
        }
        return m;
    }

    /**
     * {@code user_items.id} → {@code care_items.category_id}.
     *
     * <p>⚠️ <b>{@code ItemPort.selected} 는 지금 선택한 항목만 줍니다.</b>
     * 사용자가 항목을 지우면 그 항목의 과거 기록은 {@code categoryId} 가 비게 됩니다.
     * {@code today/} 의 {@code masterOf()} 도 같은 한계가 있습니다.
     */
    private Map<String, String> categoryIdByUserItemId(String userId) {
        Map<String, String> m = new LinkedHashMap<>();
        for (ItemPort.SelectedItem s : items.selected(userId)) {
            m.put(s.userItemId(), s.categoryId());
        }
        return m;
    }

    /** 날짜가 깨져 들어오면 500 대신 400 입니다 */
    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "날짜 형식이 올바르지 않습니다");
        }
    }
}