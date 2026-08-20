package com.youin.now.home;

import com.youin.now.checkin.CheckinPort;
import com.youin.now.footstep.FootstepService;
import com.youin.now.item.ItemPort;
import com.youin.now.note.NoteRulePort;
import com.youin.now.subtract.VerdictPort;
import com.youin.now.today.TodayAction;
import com.youin.now.today.TodayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 홈 집계 {@code NOW-HOME-001}.
 *
 * <p><b>모으기만 합니다.</b> 각 패키지가 홈에 줄 조각을 스스로 만들고
 * 여기서는 붙이기만 합니다 ({@code docs/04-ports.md}).
 *
 * <p><b>{@code VerdictPort.of()} 를 부르지 않습니다.</b> 판정 32건이 통째로 오면
 * 홈이 무거워집니다. 그래서 창구가 {@code subtractForHome} 으로 따로 나뉘어 있습니다.
 *
 * <p>⚠️ <b>아직 못 채우는 둘</b> — {@code recommendationPaused} 와
 * {@code unlock.recordedDays} 는 {@code CheckinPort} 대기입니다.
 */
@Service
@Transactional(readOnly = true)
public class HomeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 관리 항목 최소 개수. {@code ErrorCode.MIN_ITEMS_REQUIRED} 와 같은 값입니다 */
    private static final int MIN_ITEMS = 3;

    /** 주간 발견이 열리는 기록 일수 */
    private static final int WEEKLY_NEED = 7;

    /** 월간 발견이 열리는 기록 일수 */
    private static final int MONTHLY_NEED = 30;

    private final ItemPort items;
    private final CheckinPort checkins;
    private final VerdictPort verdicts;
    private final NoteRulePort noteRules;
    private final TodayService todayService;
    private final FootstepService footstepService;

    public HomeService(ItemPort items,
                       CheckinPort checkins,
                       VerdictPort verdicts,
                       NoteRulePort noteRules,
                       TodayService todayService,
                       FootstepService footstepService) {
        this.items = items;
        this.checkins = checkins;
        this.verdicts = verdicts;
        this.noteRules = noteRules;
        this.todayService = todayService;
        this.footstepService = footstepService;
    }

    public HomeRes get(String userId) {
        LocalDate today = LocalDate.now(KST);

        // TODO CheckinPort 에 recommendationPaused 가 들어오면 채웁니다 (이철희 님)
        //      users.recommendation_paused 이고 GET /me 는 이미 내려주고 있습니다
        boolean paused = false;

        var checkin = checkins.latest(userId).orElse(null);
        String state = checkin == null ? null : checkin.state();

        int itemCount = items.selected(userId).size();

        // 덜어내기 요약. 판정이 없으면 포트가 null 을 주고, 명세도 「판정 전이면 null」입니다
        // removedCount 는 포트가 reduce + skip 으로 계산해 줍니다. simplify 는 세지 않습니다
        var hs = verdicts.subtractForHome(userId, today);
        HomeRes.Subtract subtract = hs == null ? null
                : new HomeRes.Subtract(
                hs.evaluationId(),
                new HomeRes.Summary(
                        hs.summary().keep(), hs.summary().simplify(),
                        hs.summary().reduce(), hs.summary().skip(),
                        hs.summary().excluded()),
                hs.removedCount());

        var todayCard = todayService.todayForHome(userId);

        // 추천 중단이면 첫 발자국 카드를 내립니다 (규칙 3) —
        // 아무것도 안 해도 되는 날에 남의 사례를 보여 주면 부담이 됩니다
        //
        // 오늘의 케어와 같은 카테고리를 우선합니다 (규칙 2).
        // 오늘 행동이 없으면 null 을 넘겨 폴백을 태웁니다
        var footstepCard = paused ? null
                : footstepService.footstepForHome(
                userId, todayCard == null ? null : todayCard.categoryId());

        // TODO CheckinPort.stats 가 열리면 채웁니다 (이철희 님)
        //      recordedDays = COUNT(DISTINCT check_date) FROM checkins
        //      /logs/summary 의 daysRecorded 와 같은 값입니다
        int recordedDays = 0;

        return new HomeRes(
                nextStepOf(paused, itemCount, checkin != null, subtract, todayCard),
                state,
                paused,
                noteRules.careForHome(userId),
                subtract,
                todayCard,
                footstepCard,
                new HomeRes.Unlock(
                        recordedDays,
                        recordedDays >= WEEKLY_NEED,
                        recordedDays >= MONTHLY_NEED,
                        MONTHLY_NEED));
    }

    /**
     * 다음 화면 하나.
     *
     * <p><b>{@code rest} 는 순서가 아니라 우선 조건입니다.</b> 추천 중단이면
     * 어느 단계에 있든 {@code rest} 입니다.
     *
     * <p>나머지는 {@code onboarding → checkin → subtract → action → done} 순입니다.
     */
    private String nextStepOf(boolean paused, int itemCount, boolean hasCheckin,
                              HomeRes.Subtract subtract, TodayService.ForHome todayCard) {

        if (paused) return "rest";
        if (itemCount < MIN_ITEMS) return "onboarding";
        if (!hasCheckin) return "checkin";
        if (subtract == null) return "subtract";

        if (todayCard == null) return "action";
        if (TodayAction.DONE.equals(todayCard.status())) return "done";
        return "action";
    }
}