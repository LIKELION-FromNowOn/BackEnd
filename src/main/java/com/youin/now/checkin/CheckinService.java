package com.youin.now.checkin;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.safety.SafetyPort;
import com.youin.now.subtract.SubtractCondition;
import java.time.LocalDate;
import java.time.ZoneId;
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

    /** 징후 14개의 가중치 합. 명세서 {@code maxScore} */
    private static final int MAX_SCORE = 25;

    /** 직접 적은 징후 하나당 점수. 명세서 처리 규칙 1번 */
    private static final int CUSTOM_SIGNAL_SCORE = 2;

    /** 날짜 경계는 KST 자정입니다. 서버 시간대로 계산하지 않습니다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CheckinRepository checkins;
    private final CheckinSignalRepository signals;
    private final SignalWeightPort weights;
    private final SafetyPort safety;

    public CheckinService(CheckinRepository checkins, CheckinSignalRepository signals,
                          SignalWeightPort weights, SafetyPort safety) {
        this.checkins = checkins;
        this.signals = signals;
        this.weights = weights;
        this.safety = safety;
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
        Map<String, Integer> found = weights.weights(ids);

        int score = found.values().stream().mapToInt(Integer::intValue).sum()
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
            if (found.containsKey(sid)) rows.add(CheckinSignal.ofMaster(Ids.of("cs"), checkin.id(), sid));
        }
        for (String text : req.customSignalsOrEmpty()) {
            rows.add(CheckinSignal.ofCustom(Ids.of("cs"), checkin.id(), text));
        }
        signals.saveAll(rows);

        boolean proposed = signalScore >= THRESHOLD;

        return new CheckinRes(
                checkin.id(),
                condition.code(),
                signalScore,
                THRESHOLD,
                MAX_SCORE,
                proposed,
                proposed ? proposedState(condition).code() : null,
                proposed ? List.of() : null,   // 근거 문구는 마스터 징후 이름에서 옵니다. 시드 대기
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
