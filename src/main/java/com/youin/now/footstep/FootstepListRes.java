package com.youin.now.footstep;

import java.util.List;

/**
 * {@code NOW-STEP-001} 응답 봉투.
 *
 * <p>온보딩 표시를 항목마다 두지 않고 <b>맨 위에 id 배열 하나</b>로 둡니다. 명세서 형식입니다.
 *
 * <p><b>{@code onboardingIds} 와 {@code total} 은 필터와 무관하게 전체 기준</b>입니다.
 * {@code ?context=onboarding} 으로 걸러도 4건 그대로, {@code total} 도 8 그대로입니다.
 */
public record FootstepListRes(
        List<FootstepRes> footsteps,
        List<String> onboardingIds,
        int total
) {}