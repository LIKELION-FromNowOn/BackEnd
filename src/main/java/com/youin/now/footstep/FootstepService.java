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

    // ── 홈 조각 ────────────────────────────────────

    /**
     * 홈의 「첫 발자국 카드」 조각. <b>네 칸입니다</b> —
     * {@code docs/04-ports.md} 의 「id 만」은 낡았고 명세가 앞섭니다.
     *
     * <p><b>오늘의 케어와 같은 카테고리를 우선합니다</b> ({@code NOW-HOME-001} 처리 규칙 2).
     * 「우선해」이므로 같은 것이 없으면 폴백입니다 — {@code care} 와 {@code med} 는
     * 사례가 아예 없어 항상 폴백을 탑니다.
     *
     * <p><b>추천 중단 상태에서는 홈이 이것을 부르지 않습니다.</b>
     * 아무것도 안 해도 되는 날에 남의 사례를 보여 주면 부담이 됩니다.
     *
     * @param preferCategoryId 오늘의 케어 카테고리. 오늘 행동이 없으면 {@code null}
     * @return 사례가 없으면 {@code null}
     */
    public ForHome footstepForHome(String userId, String preferCategoryId) {
        List<FootstepEntity> all = footsteps.findAllByOrderByIdAsc();
        if (all.isEmpty()) return null;

        // 온보딩 것 중에서 고릅니다. 온보딩이 하나도 없으면 전체에서
        List<FootstepEntity> onboarding = all.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsOnboarding()))
                .toList();
        List<FootstepEntity> pool = onboarding.isEmpty() ? all : onboarding;

        FootstepEntity picked = pool.stream()
                .filter(e -> e.getCategoryId().equals(preferCategoryId))
                .findFirst()
                .orElse(pool.get(0));       // 같은 카테고리가 없으면 첫 번째

        return new ForHome(picked.getId(), picked.getCategoryId(),
                picked.getSituation(), picked.getFirstStep());
    }

    /** 홈이 그대로 실어 보낼 모양입니다. {@code NOW-HOME-001} 의 {@code footstepCard} 블록 */
    public record ForHome(String id, String categoryId,
                          String situation, String firstStep) { }
}