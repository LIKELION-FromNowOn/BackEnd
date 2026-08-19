package com.youin.now.checkin;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CheckinPort} 의 실제 구현. <b>{@link CheckinPortStub} 을 대체합니다.</b>
 *
 * <p>스텁과 이 클래스가 동시에 있으면 스프링이 「빈이 둘」이라고 기동을 거부합니다.
 * <b>이 파일을 넣으면서 스텁을 지웠습니다.</b>
 *
 * <p>남에게 나가는 것은 {@link CheckinPort.LatestCheckin} 뿐입니다.
 * {@link Checkin} 엔티티는 이 패키지 밖으로 내보내지 않습니다.
 */
@Component
public class CheckinPortAdapter implements CheckinPort {

    private final CheckinRepository checkins;
    private final CheckinSignalRepository signals;

    public CheckinPortAdapter(CheckinRepository checkins, CheckinSignalRepository signals) {
        this.checkins = checkins;
        this.signals = signals;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LatestCheckin> latest(String userId) {
        return checkins.findTopByUserIdOrderByCheckDateDesc(userId).map(c -> {
            List<String> ids = signals.findByCheckinId(c.id()).stream()
                    .map(CheckinSignal::signalId)
                    .filter(java.util.Objects::nonNull)     // 직접 입력은 마스터 번호가 없습니다
                    .toList();
            return new LatestCheckin(c.state(), ids, c.signalScore(),
                    c.createdAt() == null ? null : c.createdAt().toLocalDateTime());
        });
    }
}
