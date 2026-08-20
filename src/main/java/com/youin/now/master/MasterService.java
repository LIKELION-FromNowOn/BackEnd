package com.youin.now.master;

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

    /**
     * 상태 전환 임계값. {@code signals} 14건의 가중치 합이 25 이고 그 위에서 정해졌습니다.
     * <b>프론트는 표시에만 씁니다.</b>
     */
    private static final int TRANSITION_THRESHOLD = 5;

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

    /** {@code NOW-MASTER-001} 카테고리 7건 */
    public MasterRes.Categories getCategories() {
        return new MasterRes.Categories(
                categories.findAllByOrderBySortOrderAsc()
                        .stream()
                        .map(MasterRes.Category::from)
                        .toList());
    }

    /** {@code NOW-MASTER-002} 관리 항목 32건 + 하한선·근거 등급 */
    public MasterRes.CareItems getCareItems() {
        Map<String, String> names = categories.findAll().stream()
                .collect(Collectors.toMap(MasterCategory::id, MasterCategory::name));

        List<MasterRes.CareItem> items = careItems.findAllByOrderByCategoryIdAscIdAsc()
                .stream()
                .map(e -> MasterRes.CareItem.from(e, names.get(e.categoryId())))
                .toList();

        return new MasterRes.CareItems(items);
    }

    /** {@code NOW-MASTER-003} 이상 징후 14건 + 가중치·전환 임계값 */
    public MasterRes.Signals getSignals() {
        return new MasterRes.Signals(
                signals.findAllByOrderBySortOrderAsc()
                        .stream()
                        .map(MasterRes.Signal::from)
                        .toList(),
                TRANSITION_THRESHOLD);
    }
}