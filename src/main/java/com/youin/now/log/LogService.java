package com.youin.now.log;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.item.ItemPort;
import com.youin.now.master.MasterCategory;
import com.youin.now.master.MasterCategoryRepository;
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
 * <p>⚠️ <b>{@code summary} 의 네 필드가 아직 비어 있습니다.</b>
 * {@code daysRecorded} · {@code topState} 는 {@code CheckinPort},
 * {@code daysSubtracted} · {@code topSubtracted} 는 {@code VerdictPort.stats()} 대기입니다.
 */
@Service
@Transactional(readOnly = true)
public class LogService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** {@code week} 7일 · {@code month} 30일. 홈의 {@code unlock.monthlyNeed} 가 30 입니다 */
    private static final int WEEK_DAYS = 7;
    private static final int MONTH_DAYS = 30;

    private final LogActionRepository actions;
    private final ItemPort items;
    private final MasterCategoryRepository categories;

    public LogService(LogActionRepository actions,
                      ItemPort items,
                      MasterCategoryRepository categories) {
        this.actions = actions;
        this.items = items;
        this.categories = categories;
    }

    // ── NOW-LOG-001 ────────────────────────────────

    /**
     * 완료한 행동을 날짜별로.
     *
     * @param from 없으면 30일 전
     * @param to   없으면 오늘
     */
    public LogRes.Days getLogs(String userId, String from, String to) {
        LocalDate toDate = (to == null || to.isBlank())
                ? LocalDate.now(KST) : parseDate(to);
        LocalDate fromDate = (from == null || from.isBlank())
                ? toDate.minusDays(MONTH_DAYS) : parseDate(from);

        if (fromDate.isAfter(toDate)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "시작일이 종료일보다 늦습니다");
        }

        Map<String, String> names = categoryNames();
        Map<String, String> categoryOf = categoryIdByUserItemId(userId);

        // 날짜별로 묶습니다. 최신 날짜가 먼저입니다
        Map<LocalDate, List<LogRes.Item>> byDate = new LinkedHashMap<>();

        for (TodayAction a : done(userId, fromDate, toDate)) {
            LocalDate d = a.completedAt().atZoneSameInstant(KST).toLocalDate();
            String categoryId = categoryOf.get(a.userItemId());

            byDate.computeIfAbsent(d, k -> new ArrayList<>())
                    .add(new LogRes.Item(
                            a.id(),
                            a.title(),
                            categoryId,
                            categoryId == null ? null : names.get(categoryId),
                            a.startedAt() != null));
        }

        List<LogRes.Day> days = new ArrayList<>();
        byDate.forEach((d, logs) -> days.add(new LogRes.Day(d.toString(), logs)));

        return new LogRes.Days(days);
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
            String categoryId = categoryOf.get(a.userItemId());
            String name = categoryId == null ? null : names.get(categoryId);
            if (name == null) continue;
            counts.merge(name, 1, Integer::sum);
        }

        List<LogRes.CategoryCount> byCategory = new ArrayList<>();
        counts.forEach((name, c) -> byCategory.add(new LogRes.CategoryCount(name, c)));

        // TODO CheckinPort 가 열리면 채웁니다 (이철희 님)
        //      daysRecorded = COUNT(DISTINCT check_date) FROM checkins
        //      topState     = checkins.state 최빈값
        //      홈의 unlock.recordedDays 와 같은 값입니다
        int daysRecorded = 0;
        String topState = null;

        // TODO VerdictPort.stats(userId, from, to) 가 머지되면 채웁니다 (송원석 님)
        //      → { daysSubtracted, topSubtracted: [{ itemId, count }] }
        //      이름은 안 담겨 오니 care_items 에서 붙입니다
        int daysSubtracted = 0;
        List<LogRes.ItemCount> topSubtracted = List.of();

        return new LogRes.Summary(
                p, rows.size(), byCategory, sentenceOf(p, byCategory),
                daysRecorded, daysSubtracted, topState, topSubtracted);
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
        if (last < 0xAC00 || last > 0xD7A3) return "를";        // 한글이 아니면
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