package com.youin.now.master;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 마스터 3건 조회. <b>계산도 판정도 없습니다.</b> 시드가 들어간 테이블을 그대로 내려줍니다.
 *
 * <p>⚠️ {@link SignalWeightAdapter} 와 다른 일을 합니다. 그쪽은 {@code @Component} 로
 * {@code checkin/} 에 가중치를 넘기고, 이쪽은 {@code @Service} 로 응답을 만듭니다.
 */
@Service
@Transactional(readOnly = true)
public class MasterService {

    /** 상태 전환 임계값. 가중치 합 25 위에서 정해졌습니다 */
    private static final int THRESHOLD = 5;

    /** 직접 적은 징후 하나당 가중치 */
    private static final int CUSTOM_WEIGHT = 2;

    /** 직접 적을 수 있는 최대 개수 */
    private static final int CUSTOM_MAX = 5;

    /** 화면 그룹 순서. <b>없으면 징후 선택 화면을 못 그립니다</b> */
    private static final List<String> GROUPS =
            List.of("피부", "수면", "마음", "관계", "생활");

    private final MasterCategoryRepository categories;
    private final MasterCareItemRepository careItems;
    private final MasterSignalRepository signals;

    public MasterService(MasterCategoryRepository categories,
                         MasterCareItemRepository careItems,
                         MasterSignalRepository signals) {
        this.categories = categories;
        this.careItems = careItems;
        this.signals = signals;
    }

    /** {@code NOW-MASTER-001} 카테고리 7건. {@code itemCount} 는 분류별 항목 수입니다 */
    public List<MasterRes.Category> getCategories() {
        Map<String, Long> counts = careItems.findAll().stream()
                .collect(Collectors.groupingBy(MasterCareItem::categoryId, Collectors.counting()));

        return categories.findAllByOrderBySortOrderAsc().stream()
                .map(c -> MasterRes.Category.from(c, counts.getOrDefault(c.id(), 0L).intValue()))
                .toList();
    }

    /**
     * {@code NOW-MASTER-002} 관리 항목 32건.
     *
     * @param category 없으면 전부. <b>없는 카테고리면 400</b> — 조용히 빈 목록을 주면
     *                 프론트가 자기 오타를 「항목이 없구나」로 읽습니다
     */
    public List<MasterRes.CareItem> getCareItems(String category) {
        Map<String, String> names = categories.findAll().stream()
                .collect(Collectors.toMap(MasterCategory::id, MasterCategory::name));

        if (category != null && !category.isBlank() && !names.containsKey(category)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 카테고리입니다");
        }

        List<MasterCareItem> rows = (category == null || category.isBlank())
                ? careItems.findAllByOrderByCategoryIdAscIdAsc()
                : careItems.findAllByCategoryIdOrderByIdAsc(category);

        return rows.stream()
                .map(e -> MasterRes.CareItem.from(e, names.get(e.categoryId())))
                .toList();
    }

    /** {@code NOW-MASTER-003} 이상 징후 14건 + 가중치·임계값·그룹 */
    public MasterRes.Signals getSignals() {
        List<MasterSignal> rows = signals.findAllByOrderBySortOrderAsc();

        // 시드가 바뀌어도 자동으로 맞도록 상수가 아니라 실제 합을 씁니다
        int maxScore = rows.stream().mapToInt(MasterSignal::weight).sum();

        return new MasterRes.Signals(
                rows.stream().map(MasterRes.Signal::from).toList(),
                THRESHOLD,
                maxScore,
                CUSTOM_WEIGHT,
                CUSTOM_MAX,
                GROUPS);
    }
}