package com.youin.now.master;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** {@code categories} 조회. 읽기만 합니다. */
public interface MasterCategoryRepository extends JpaRepository<MasterCategory, String> {

    List<MasterCategory> findAllByOrderBySortOrderAsc();
}