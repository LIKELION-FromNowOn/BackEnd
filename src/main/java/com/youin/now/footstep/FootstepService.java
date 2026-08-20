package com.youin.now.footstep;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FootstepService {

    private static final String CTX_ONBOARDING = "onboarding";
    private static final String CTX_HOME = "home";

    private final FootstepRepository footsteps;
    private final FootstepCategoryRepository categories;

    public FootstepService(FootstepRepository footsteps,
                           FootstepCategoryRepository categories) {
        this.footsteps = footsteps;
        this.categories = categories;
    }

    /**
     * {@code NOW-STEP-001} 사례 목록.
     *
     * <p><b>{@code onboardingIds} 와 {@code total} 은 필터와 무관하게 전체 기준</b>입니다.
     * 명세서가 그렇게 정했고, 판정 API 의 {@code summary} 와 같은 규칙입니다.
     *
     * <p>모르는 값이 오면 400 입니다. 조용히 무시하면 프론트가 자기 오타를
     * 「데이터가 없구나」로 읽습니다.
     *
     * @param context    {@code onboarding} 이면 온보딩 4건만. {@code home} 과 없음은 전체.
     *                   홈 카드는 클라이언트가 골라 쓰므로 서버는 전부 줍니다
     * @param categoryId 없으면 전부
     */
    public FootstepListRes getFootsteps(String context, String categoryId) {

        if (context != null && !context.isBlank()
                && !CTX_ONBOARDING.equals(context) && !CTX_HOME.equals(context)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "context 값이 올바르지 않습니다");
        }

        Map<String, String> names = categories.findAll().stream()
                .collect(Collectors.toMap(FootstepCategory::getId, FootstepCategory::getName));

        if (categoryId != null && !categoryId.isBlank() && !names.containsKey(categoryId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "카테고리를 찾을 수 없습니다");
        }

        List<FootstepEntity> all = footsteps.findAllByOrderByIdAsc();

        // ★ 필터 전에 만듭니다. 「무엇이 온보딩인가」를 알려주는 값이라 필터와 무관합니다
        List<String> onboardingIds = all.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsOnboarding()))
                .map(FootstepEntity::getId)
                .toList();
        int total = all.size();

        List<FootstepEntity> rows = all.stream()
                .filter(e -> categoryId == null || categoryId.isBlank()
                        || categoryId.equals(e.getCategoryId()))
                .filter(e -> !CTX_ONBOARDING.equals(context)
                        || Boolean.TRUE.equals(e.getIsOnboarding()))
                .toList();

        List<FootstepRes> items = rows.stream()
                .map(e -> FootstepRes.from(e, names.get(e.getCategoryId())))
                .toList();

        return new FootstepListRes(items, onboardingIds, total);
    }
}