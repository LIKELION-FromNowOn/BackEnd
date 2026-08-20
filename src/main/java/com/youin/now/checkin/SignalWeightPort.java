package com.youin.now.checkin;

import java.util.Map;
import java.util.Set;

/**
 * 이상 징후의 가중치를 읽는 창구. <b>{@code master/} 가 제공합니다 — 김민정 님 패키지입니다.</b>
 *
 * <p>남의 {@code Repository} 를 직접 부르지 않는다는 규칙({@code docs/04-ports.md:3})에 따라
 * 창구를 하나 둡니다. {@code checkin/} 은 이 인터페이스만 압니다.
 *
 * <p><b>2026-08-20 실제 구현이 붙었습니다.</b> 마스터 시드가 들어와서
 * {@code master/SignalWeightAdapter} 가 {@code signals} 를 직접 읽습니다.
 * 그전에는 스텁이 빈 지도를 돌려줘 신호 강도가 항상 0 이었고,
 * 임계값 5 를 못 넘어 <b>상태 전환 제안이 한 번도 뜨지 않았습니다.</b>
 */
public interface SignalWeightPort {

    /**
     * @param signalIds 사용자가 고른 마스터 징후 번호들
     * @return 번호 → 가중치. <b>없는 번호는 결과에서 빠집니다.</b> 예외를 던지지 않습니다
     */
    Map<String, Integer> weights(Set<String> signalIds);
}
