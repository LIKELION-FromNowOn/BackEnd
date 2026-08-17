package com.youin.now.subtract;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * <b>임시 스텁입니다. 「오늘 판정이 아직 없다」고 답합니다.</b>
 *
 * <p>부르는 쪽은 이 상태에서도 화면이 끝까지 돌아가야 합니다.
 * 진짜 구현({@code SubtractVerdictService})이 들어오면 <b>이 파일을 삭제</b>하십시오.
 */
@Primary
@Component
public class VerdictPortStub implements VerdictPort {

    @Override
    public Optional<VerdictSet> of(Long userId, LocalDate date) {
        return Optional.empty();
    }

    @Override
    public Summary summary(Long userId, LocalDate date) {
        return new Summary(0, 0, 0, 0, 0);
    }
}
