package com.youin.now.checkin;

import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * <b>임시 구현입니다. 상태 API 가 들어오면 이 파일을 지우십시오.</b>
 *
 * <p>왜 인터페이스만 올리지 않고 이것까지 만들었는가 —
 * <b>인터페이스만 있으면 주입하는 순간 앱이 안 뜹니다.</b>
 * {@code NoSuchBeanDefinitionException} 이 나서, 부르는 쪽이 코드를 쓸 수가 없습니다.
 * 이 스텁이 있으면 <b>{@code home/} · {@code today/} 를 지금 바로 진행할 수 있습니다.</b>
 *
 * <p>항상 {@link Optional#empty()} 를 돌려줍니다. 부르는 쪽에서는
 * <b>「이 사람이 아직 오늘 상태를 안 골랐다」</b>로 읽으면 됩니다.
 * 지어낸 값을 돌려주지 않습니다 — 가짜 상태로 만든 화면은 나중에 다시 만들어야 합니다.
 *
 * <p><b>홈에서는 이것이 정상 경로입니다.</b> {@code nextStep} 이 {@code checkin} 이 되어
 * 「오늘 상태부터 골라 주세요」 화면으로 갑니다.
 *
 * <hr>
 *
 * <p><b>실제 구현을 만드실 때</b> — 이 파일을 <b>지우고</b> 같은 인터페이스를 구현한
 * {@code @Component} 를 하나만 두십시오. 둘이 동시에 있으면 스프링이
 * 「빈이 둘」이라고 기동을 거부합니다. <b>그때 이 주석을 떠올리시면 원인을 바로 찾습니다.</b>
 *
 * <p>작성 2026-08-20 · 근거 {@code .agent/REQUESTS.md} #19
 */
@Component
public class CheckinPortStub implements CheckinPort {

    @Override
    public Optional<LatestCheckin> latest(String userId) {
        return Optional.empty();
    }
}
