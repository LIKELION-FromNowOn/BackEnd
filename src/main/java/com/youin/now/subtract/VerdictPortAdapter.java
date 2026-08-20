package com.youin.now.subtract;

import com.youin.now.item.ItemPort;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link VerdictPort} 실제 구현 — <b>{@code today/} 와 {@code home/} 이 판정 결과를 읽는 창구</b>입니다.
 *
 * <p><b>2026-08-20 {@code VerdictPortStub} 을 지우고 이것으로 바꿨습니다.</b> 스텁이
 * {@code Optional.empty()} 와 {@code Summary(0,0,0,0,0)} 을 돌려줘서, 김민정 님이
 * {@code today/} 와 {@code home/} 을 붙여도 <b>화면이 조용히 비었습니다.</b>
 *
 * <h2>왜 메서드가 둘인가</h2>
 *
 * <p>{@code docs/04-ports.md} 가 정한 것입니다. 홈은 판정 32건이 아니라 <b>개수 다섯 개</b>만
 * 필요합니다. 같은 메서드를 쓰면 홈이 열릴 때마다 32건이 통째로 갑니다.
 * <b>응답 다이어트를 인터페이스 수준에서 강제하는 장치</b>입니다.
 *
 * <h2>날짜 경계는 KST 자정입니다</h2>
 *
 * <p>{@code evaluations.created_at} 은 {@code DATETIME(6)} 이라 시간대를 저장하지 않습니다.
 * <b>「오늘」을 서버 시간대로 계산하면 하루가 어긋납니다.</b> 여기서 {@code Asia/Seoul} 로
 * 하루의 시작과 끝을 만들어 넘깁니다 — {@code checkin/} 의 {@code check_date} 와 같은 규칙입니다.
 *
 * <h2>돌려주는 {@code itemId} 는 마스터 ID 입니다</h2>
 *
 * <p>저장은 {@code user_items.id} 로 되어 있는데(외래키가 그것을 요구합니다),
 * <b>부르는 쪽이 쓰기 좋은 것은 마스터 ID</b>({@code cr4})입니다. 명세서의 응답도 그쪽이고,
 * {@code today/} 가 항목 이름을 찾으려면 마스터 ID 가 있어야 합니다. 그래서 여기서 옮깁니다.
 *
 * <p><b>사용자가 판정 뒤에 항목을 지우면</b> 그 항목만 옮기지 못하고 저장된 값을 그대로 씁니다.
 * 판정 자체는 그대로 남습니다.
 *
 * <p>⚠️ <b>{@code ItemPort} 가 아직 스텁이라 지금은 {@code itemId} 자리에 {@code userItemId} 가 그대로 옵니다.</b>
 * 이철희 님이 실제 구현을 넣으면 저절로 마스터 ID 로 바뀝니다.
 * <b>{@code userItemId} 와 {@code evaluationId} 는 지금도 정확합니다</b> — 저장된 값을 그대로 쓰기 때문입니다.
 * {@code today/} 가 {@code actions} 행을 만드는 데 필요한 것은 그 둘입니다.
 */
@Component
public class VerdictPortAdapter implements VerdictPort {

    /** 「오늘」의 기준. 서버 시간대가 아니라 이것을 씁니다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final EvaluationRepository evaluations;
    private final EvaluationResultRepository results;
    private final ItemPort items;

    public VerdictPortAdapter(EvaluationRepository evaluations,
                              EvaluationResultRepository results,
                              ItemPort items) {
        this.evaluations = evaluations;
        this.results = results;
        this.items = items;
    }

    /**
     * @return 그날 판정이 없으면 <b>{@code Optional.empty()}</b>. 빈 목록이 아닙니다 —
     *         「판정이 없다」와 「판정했는데 결과가 0건이다」는 뜻이 다릅니다
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<VerdictSet> of(String userId, LocalDate date) {
        return evaluations.findOfDate(userId, startOf(date), startOf(date.plusDays(1)))
                .map(ev -> {
                    Map<String, String> master = masterIdByUserItemId(userId);
                    List<ItemVerdict> out = new ArrayList<>();
                    for (EvaluationResult r : results.findByEvaluationId(ev.id())) {
                        out.add(new ItemVerdict(
                                r.userItemId(),
                                master.getOrDefault(r.userItemId(), r.userItemId()),
                                r.verdict(), r.reason(), r.excludedBy()));
                    }
                    return new VerdictSet(ev.id(), out);
                });
    }

    /**
     * 홈이 쓰는 얇은 것.
     *
     * @return 그날 판정이 없으면 <b>전부 0</b>. 홈은 카드를 안 띄우면 되고,
     *         이 자리에서 예외를 던지면 홈 전체가 죽습니다
     */
    @Override
    @Transactional(readOnly = true)
    public Summary summary(String userId, LocalDate date) {
        return evaluations.findOfDate(userId, startOf(date), startOf(date.plusDays(1)))
                .map(ev -> {
                    int keep = 0, simplify = 0, reduce = 0, skip = 0, excluded = 0;
                    for (EvaluationResult r : results.findByEvaluationId(ev.id())) {
                        switch (r.verdict()) {
                            case "keep"     -> keep++;
                            case "simplify" -> simplify++;
                            case "reduce"   -> reduce++;
                            case "skip"     -> skip++;
                            case "excluded" -> excluded++;
                            default -> { }
                        }
                    }
                    return new Summary(keep, simplify, reduce, skip, excluded);
                })
                .orElseGet(() -> new Summary(0, 0, 0, 0, 0));
    }

    /** KST 그날 00:00. {@code created_at} 이 시간대 없는 컬럼이라 여기서 만들어 넘깁니다. */
    private static OffsetDateTime startOf(LocalDate date) {
        return date.atStartOfDay(KST).toOffsetDateTime();
    }

    /** {@code user_items.id} → {@code care_items.id}. 없는 것은 빠집니다. */
    private Map<String, String> masterIdByUserItemId(String userId) {
        Map<String, String> m = new HashMap<>();
        for (ItemPort.SelectedItem s : items.selected(userId)) m.put(s.userItemId(), s.itemId());
        return m;
    }

    /**
     * {@code NOW-LOG-002} 의 {@code daysSubtracted} · {@code topSubtracted}.
     *
     * <p>이름은 안 담습니다 — {@code care_items} 는 {@code master/} 소유입니다.
     */
    @Override
    public Stats stats(String userId, LocalDate from, LocalDate to) {
        OffsetDateTime f = from == null ? null : from.atStartOfDay(KST).toOffsetDateTime();
        OffsetDateTime t = to == null ? null : to.plusDays(1).atStartOfDay(KST).toOffsetDateTime();

        List<TopItem> top = new ArrayList<>();
        for (EvaluationResultRepository.TopRow r : results.findTopSubtracted(userId, f, t)) {
            top.add(new TopItem(r.getItemId(), r.getCnt()));
        }
        return new Stats(evaluations.countSubtractedDays(userId, f, t), top);
    }

    /**
     * 홈의 {@code subtract} 블록. <b>그날 판정이 없으면 {@code null} 입니다.</b>
     *
     * <p>{@code removedCount} 는 {@code reduce + skip} 입니다.
     * {@code simplify} 는 방식만 바꾼 것이라 걷어낸 수에 안 넣습니다.
     */
    @Override
    @Transactional(readOnly = true)
    public HomeSubtract subtractForHome(String userId, LocalDate date) {
        return evaluations.findOfDate(userId, startOf(date), startOf(date.plusDays(1)))
                .map(ev -> {
                    Summary s = summary(userId, date);
                    return new HomeSubtract(ev.id(), s, s.reduce() + s.skip());
                })
                .orElse(null);
    }
}
