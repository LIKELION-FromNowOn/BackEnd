package com.youin.now.master;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** {@code care_items} 조회. 읽기만 합니다. */
public interface MasterCareItemRepository extends JpaRepository<MasterCareItem, String> {

    /** {@code care_items} 에 {@code sort_order} 가 없어 카테고리·id 순입니다 */
    List<MasterCareItem> findAllByOrderByCategoryIdAscIdAsc();
}