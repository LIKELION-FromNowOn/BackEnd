package com.youin.now.master;

import com.youin.now.checkin.SignalWeightPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@link SignalWeightPort} 실제 구현 — {@code signals} 마스터를 읽습니다.
 *
 * <p><b>2026-08-20 {@code SignalWeightPortStub} 을 지우고 이것으로 바꿨습니다.</b>
 * 스텁이 빈 지도를 돌려줘서 {@code signalScore} 가 항상 0 이었고,
 * 임계값 5 를 못 넘어 상태 전환 제안이 한 번도 뜨지 않았습니다.
 * 스텁 주석에 「시드가 들어오면 그대로 값이 흐릅니다」로 적어 뒀고,
 * 마스터 시드가 같은 날 들어갔습니다.
 *
 * <p><b>없는 번호는 결과에서 그냥 빠집니다.</b> 예외를 던지지 않습니다 —
 * 프론트가 옛 징후 번호를 들고 있어도 체크인 자체는 성공해야 합니다.
 * 규약은 {@link SignalWeightPort} 에 그렇게 적혀 있습니다.
 *
 * <p>⚠️ <b>{@code master/} 는 김민정 님 폴더입니다.</b> 사연은 {@link MasterSignal} 주석에 있습니다.
 * <b>같은 일을 하는 {@code @Component} 를 또 만드시면 빈이 둘이라 앱이 안 뜹니다.</b>
 */
@Component
public class SignalWeightAdapter implements SignalWeightPort {

    private final MasterSignalRepository signals;

    public SignalWeightAdapter(MasterSignalRepository signals) {
        this.signals = signals;
    }

    @Override
    public Map<String, Integer> weights(Set<String> signalIds) {
        if (signalIds == null || signalIds.isEmpty()) return Map.of();
        Map<String, Integer> out = new HashMap<>();
        for (MasterSignal s : signals.findByIdIn(signalIds)) {
            out.put(s.id(), (int) s.weight());
        }
        return out;
    }
}
