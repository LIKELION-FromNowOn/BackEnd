package com.youin.now.subtract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 오늘의 덜어내기 판정 결과 조회.
 *
 * <p><b>제공 — 송원석 · 사용 — 김민정(today, home)</b>
 *
 * <p><b>{@link #summary} 가 따로 있는 이유</b> — 홈은 판정 전체가 아니라 개수 다섯 개만 필요합니다.
 * 같은 메서드를 쓰면 홈이 열릴 때마다 판정 수십 건이 통째로 옵니다.
 * <b>응답 다이어트를 인터페이스 수준에서 강제하는 장치입니다.</b> 홈에서 {@link #of} 를 부르지 마십시오.
 *
 * <p>시그니처 원본은 {@code docs/04-ports.md} 입니다. <b>여기서 바꾸지 마십시오.</b>
 */
public interface VerdictPort {

    /**
     * @return 그날 판정이 아직 없으면 {@link Optional#empty()}
     */
    Optional<VerdictSet> of(Long userId, LocalDate date);

    /**
     * 홈 전용. 판정이 없으면 <b>전부 0 인 Summary</b> 를 돌려줍니다 (null 아님).
     */
    Summary summary(Long userId, LocalDate date);

    record VerdictSet(List<ItemVerdict> results) {}

    /**
     * @param verdict    keep | simplify | reduce | skip | excluded
     * @param excludedBy excluded 일 때만. floor | clinicNote. 아니면 null
     */
    record ItemVerdict(String itemId, String verdict,
                       String reason, String excludedBy) {}

    record Summary(int keep, int simplify, int reduce,
                   int skip, int excluded) {}
}
