package com.youin.now.checkin;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * <b>임시 구현입니다. 마스터 시드와 {@code master/} 조회가 들어오면 이 파일을 지우십시오.</b>
 *
 * <p>빈 지도를 돌려줍니다. 그래서 마스터 징후를 몇 개 고르든 가중치 합이 <b>0</b> 이 되고,
 * 전환 제안 임계값 5 를 넘지 않습니다. <b>지어낸 가중치를 넣지 않았습니다</b> —
 * 가짜 점수로 만든 전환 제안은 검증할 수가 없습니다.
 *
 * <p><b>직접 입력 징후는 이 창구를 타지 않습니다.</b> 명세서가 「직접 적은 징후는 각 2점」으로
 * 정해 두었고 그 값은 마스터와 무관합니다. 그래서 직접 입력만으로도 점수가 오릅니다.
 *
 * <p>작성 2026-08-20 · 근거 {@code .agent/REQUESTS.md} #19
 */
@Component
public class SignalWeightPortStub implements SignalWeightPort {

    @Override
    public Map<String, Integer> weights(Set<String> signalIds) {
        return Map.of();
    }
}
