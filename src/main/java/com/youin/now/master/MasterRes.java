package com.youin.now.master;

import java.math.BigDecimal;
import java.util.List;

/**
 * 마스터 3건 응답 {@code NOW-MASTER-001~003}.
 *
 * <p><b>{@code /categories} 와 {@code /care-items} 는 {@code data} 가 바로 배열입니다.</b>
 * 프론트가 {@code http<any[]>('GET', '/categories')} 로 배열을 기다립니다.
 *
 * <p>{@code /signals} 만 객체입니다 — {@code threshold} 처럼 목록 밖의 값이 있어서입니다.
 */
public final class MasterRes {

    private MasterRes() { }

    // ── NOW-MASTER-001 ────────────────────────────────

    /**
     * @param order     노출 순서 1~7
     * @param itemCount 분류별 항목 수
     */
    public record Category(String id, String name, short order, int itemCount) {

        public static Category from(MasterCategory e, int itemCount) {
            return new Category(e.id(), e.name(), e.sortOrder(), itemCount);
        }
    }

    // ── NOW-MASTER-002 ────────────────────────────────

    /**
     * @param category 카테고리 id. <b>{@code categoryId} 가 아닙니다</b>
     */
    public record CareItem(
            String id,
            String category,
            String categoryName,
            String name,
            String floor,
            String evidenceLevel,
            BigDecimal core,
            BigDecimal base,
            short minutes,
            boolean frequencyEditable,
            String defaultFrequency
    ) {
        public static CareItem from(MasterCareItem e, String categoryName) {
            return new CareItem(
                    e.id(),
                    e.categoryId(),
                    categoryName,
                    e.name(),
                    e.floor(),
                    e.evidenceLevel(),
                    e.core(),
                    e.base(),
                    e.minutes(),
                    e.frequencyEditable(),
                    e.defaultFrequency()
            );
        }
    }

    // ── NOW-MASTER-003 ────────────────────────────────

    /**
     * {@code /signals} 만 객체입니다.
     *
     * @param threshold    상태 전환 임계값
     * @param maxScore     전체 가중치 합
     * @param customWeight 직접 적은 징후 하나당
     * @param customMax    직접 적을 수 있는 최대
     * @param groups       화면 그룹 순서. <b>없으면 징후 선택 화면을 못 그립니다</b>
     */
    public record Signals(
            List<Signal> signals,
            int threshold,
            int maxScore,
            int customWeight,
            int customMax,
            List<String> groups
    ) { }

    public record Signal(String id, String group, String name, short weight) {

        public static Signal from(MasterSignal e) {
            return new Signal(e.id(), e.groupName(), e.name(), e.weight());
        }
    }
}