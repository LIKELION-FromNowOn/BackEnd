package com.youin.now.checkin;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.safety.SafetyPort;
import com.youin.now.subtract.SubtractCondition;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오늘 상태 체크. <b>규칙 기반이고 LLM 을 쓰지 않습니다.</b>
 *
 * <p>명세서 {@code NOW-STATE-001} 처리 규칙 5번이 「같은 입력에는 항상 같은 결과」입니다.
 * 그래서 이 자리에 AI 를 두지 않았습니다.
 *
 * <p><b>하루 한 건입니다.</b> 같은 날 다시 제출하면 새 행을 만들지 않고 덮어씁니다
 * ({@code ux_checkins_user_date} 가 강제합니다).
 */
@Service
public class CheckinService {

    /** 전환 제안 임계값. <b>명세서 값이며 코드에서 바꾸지 마십시오.</b> */
    private static final int THRESHOLD = 5;

    /** 조회 응답도 같은 임계값을 써야 해서 밖으로 엽니다. */
    public static int threshold() { return THRESHOLD; }

    /** 징후 14개의 가중치 합. 명세서 {@code maxScore} */
    private static final int MAX_SCORE = 25;

    /** 직접 적은 징후 하나당 점수. 명세서 처리 규칙 1번 */
    private static final int CUSTOM_SIGNAL_SCORE = 2;

    /** 거절 뒤 같은 신호 조합을 다시 제안하지 않는 기간. 2026-08-20 송원석 결정. */
    private static final long REPROPOSAL_COOLDOWN_DAYS = 3;

    /** 날짜 경계는 KST 자정입니다. 서버 시간대로 계산하지 않습니다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CheckinRepository checkins;
    private final CheckinSignalRepository signals;
    private final SignalWeightPort weights;
    private final SafetyPort safety;
    private final CheckinStateTransitionRepository transitions;

    public CheckinService(CheckinRepository checkins, CheckinSignalRepository signals,
                          SignalWeightPort weights, SafetyPort safety,
                          CheckinStateTransitionRepository transitions) {
        this.checkins = checkins;
        this.signals = signals;
        this.weights = weights;
        this.safety = safety;
        this.transitions = transitions;
    }

    @Transactional
    public CheckinRes submit(String userId, CheckinReq req) {

        SubtractCondition condition = parse(req.state());

        // 직접 입력은 저장 전에 위기 신호 검사를 통과해야 합니다.
        // 프롬프트로 막지 않습니다 — docs/prompts/00-index.md
        for (String text : req.customSignalsOrEmpty()) {
            SafetyPort.SafetyResult r = safety.check(text, SafetyPort.Source.SIGNAL_CUSTOM);
            if (r.blocked()) {
                throw new ApiException(ErrorCode.TEXT_REJECTED, r.message());
            }
        }

        LocalDate today = LocalDate.now(KST);

        // 마스터 징후는 중복을 지웁니다. 같은 것을 두 번 고르면 점수가 두 배가 됩니다
        Set<String> ids = new LinkedHashSet<>(req.signalIdsOrEmpty());
        List<SignalWeightPort.SignalInfo> found = weights.find(ids);

        int score = found.stream().mapToInt(SignalWeightPort.SignalInfo::weight).sum()
                  + req.customSignalsOrEmpty().size() * CUSTOM_SIGNAL_SCORE;
        short signalScore = (short) Math.min(score, MAX_SCORE);

        Checkin checkin = checkins.findByUserIdAndCheckDate(userId, today)
                .map(c -> { c.update(condition.code(), condition.judgeStrength(), signalScore); return c; })
                .orElseGet(() -> new Checkin(Ids.checkin(), userId, today,
                        condition.code(), condition.judgeStrength(), signalScore));
        checkins.save(checkin);

        // 다시 제출하면 징후도 새로 씁니다. 지우고 넣습니다
        signals.deleteByCheckinId(checkin.id());
        List<CheckinSignal> rows = new ArrayList<>();
        for (String sid : ids) {
            if (found.stream().anyMatch(f -> f.id().equals(sid)))
                rows.add(CheckinSignal.ofMaster(Ids.of("cs"), checkin.id(), sid));
        }
        for (String text : req.customSignalsOrEmpty()) {
            rows.add(CheckinSignal.ofCustom(Ids.of("cs"), checkin.id(), text));
        }
        signals.saveAll(rows);

        boolean proposed = createProposalIfEligible(userId, condition, signalScore);

        return new CheckinRes(
                checkin.id(),
                condition.code(),
                signalScore,
                THRESHOLD,
                MAX_SCORE,
                proposed,
                proposed ? proposedState(condition).code() : null,
                // 명세서의 reasons — 고른 마스터 징후 이름을 가중치 큰 것부터.
                // 2026-08-20 까지 빈 배열이었습니다. 근거 없는 전환 제안이 화면에 떴습니다.
                // 직접 입력한 것은 넣지 않습니다 — 명세 예시가 마스터 이름만 보여 줍니다
                proposed ? found.stream().map(SignalWeightPort.SignalInfo::name).toList() : null,
                proposed ? null : "신호 강도 " + signalScore + " / " + MAX_SCORE
                                  + " — 아직 상태 전환으로 판단하지 않습니다.",
                condition.recommendationPaused(),
                condition.judgeStrength());
    }

    /** 판정 전에 「상태 체크가 있는가」를 보는 자리에서 씁니다. */
    @Transactional(readOnly = true)
    public Optional<Checkin> latest(String userId) {
        return checkins.findTopByUserIdOrderByCheckDateDesc(userId);
    }

    /** 전환 제안을 수락하면 상태와 추천 중단 여부를 갱신하고, 거절하면 3일 유예를 계산해 반환합니다. */
    @Transactional
    public CheckinTransitionRes respondTransition(String userId, CheckinTransitionReq req) {
        if (req.checkinId() == null || req.checkinId().isBlank() || req.accept() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
        Checkin checkin = checkins.findByIdAndUserId(req.checkinId(), userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHECKIN_NOT_FOUND));
        CheckinStateTransition transition = transitions.findTopByUserIdAndAcceptedIsNullOrderByCreatedAtDesc(userId)
                .filter(t -> t.fromState().equals(checkin.state()) && checkin.signalScore() >= THRESHOLD)
                .orElseThrow(() -> new ApiException(ErrorCode.NO_PROPOSAL));

        OffsetDateTime respondedAt = OffsetDateTime.now(KST);
        boolean accepted = req.accept();
        transition.respond(accepted, respondedAt);
        if (accepted) {
            SubtractCondition target = SubtractCondition.of(transition.toState());
            checkin.transitionTo(target.code(), target.judgeStrength());
            boolean paused = target.recommendationPaused();
            transitions.updateRecommendationPaused(userId, paused);
            return new CheckinTransitionRes(target.code(), true, paused, true, null);
        }
        String blockedUntil = respondedAt.plusDays(REPROPOSAL_COOLDOWN_DAYS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new CheckinTransitionRes(checkin.state(), false, false, null, blockedUntil);
    }

    private boolean createProposalIfEligible(String userId, SubtractCondition condition, short signalScore) {
        if (signalScore < THRESHOLD) {
            transitions.deleteByUserIdAndAcceptedIsNull(userId);
            return false;
        }
        if (transitions.findTopByUserIdAndAcceptedIsNullOrderByCreatedAtDesc(userId).isPresent()) return true;
        boolean blocked = transitions.findTopByUserIdAndAcceptedFalseAndRespondedAtIsNotNullOrderByRespondedAtDesc(userId)
                .map(t -> t.respondedAt().plusDays(REPROPOSAL_COOLDOWN_DAYS).isAfter(OffsetDateTime.now(KST)))
                .orElse(false);
        if (blocked) return false;
        transitions.save(new CheckinStateTransition(Ids.of("st"), userId, condition.code(),
                proposedState(condition).code(), signalScore));
        return true;
    }

    /**
     * 알 수 없는 상태 값이면 400. <b>{@code unknown} 은 정상 값이라 여기서 안 걸립니다.</b>
     * 명세서 처리 규칙 6번 — {@code unknown} 이어도 409 를 반환하지 않고 그대로 진행합니다.
     */
    private SubtractCondition parse(String state) {
        try {
            return SubtractCondition.of(state);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "상태 값이 올바르지 않습니다");
        }
    }

    /**
     * 전환 제안 상태 — 한 단계 아래로 내립니다.
     *
     * <p><b>{@code unknown} 에서는 제안하지 않고 그대로 둡니다.</b> 「모르겠다」에 답을 강요하지 않습니다.
     */
    private SubtractCondition proposedState(SubtractCondition now) {
        return switch (now) {
            case ENERGETIC -> SubtractCondition.NORMAL;
            case NORMAL    -> SubtractCondition.LOW;
            case LOW       -> SubtractCondition.DRAINED;
            case DRAINED   -> SubtractCondition.DRAINED;
            case UNKNOWN   -> SubtractCondition.UNKNOWN;
        };
    }
}
