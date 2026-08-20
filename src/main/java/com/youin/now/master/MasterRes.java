package com.youin.now.master;

import java.math.BigDecimal;
import java.util.List;

/**
 * 마스터 3건 응답 {@code NOW-MASTER-001~003}.
 *
 * <p><b>id 필드 이름이 {@code id} 가 아닙니다.</b> 프론트 목({@code src/api/master.js})이
 * {@code categoryId} · {@code itemId} · {@code signalId} 를 기다립니다.
 *
 * <p>세 응답 모두 <b>배열을 봉투에 한 번 더 감쌉니다</b> —
 * {@code categories} · {@code careItems} · {@code signals}.
 */
public final class MasterRes {

    private MasterRes() { }

    // ── NOW-MASTER-001 ────────────────────────────────

    public record Categories(List<Category> categories) { }

    public record Category(String categoryId, String name) {
        public static Category from(MasterCategory e) {
            return new Category(e.id(), e.name());
        }
    }

    // ── NOW-MASTER-002 ────────────────────────────────

    public record CareItems(List<CareItem> careItems) { }

    public record CareItem(
            String itemId,
            String categoryId,
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

    /** 임계값은 서버가 정합니다. 프론트는 계산에 쓰지 않고 표시에만 씁니다 */
    public record Signals(List<Signal> signals, int transitionThreshold) { }

    public record Signal(String signalId, String group, String name, short weight) {
        public static Signal from(MasterSignal e) {
            return new Signal(e.id(), e.groupName(), e.name(), e.weight());
        }
    }
}