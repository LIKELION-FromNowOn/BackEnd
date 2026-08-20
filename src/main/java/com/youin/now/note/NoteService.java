package com.youin.now.note;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 안내문 원문과 그 제한을 읽습니다.
 *
 * <p><b>이 앱의 차별점이 여기 걸려 있습니다.</b> 「클리닉 안내가 생활 제안보다 앞선다」는 것을
 * 코드로 만든 자리이고, 그 근거가 <b>원문 문장 번호</b>입니다.
 *
 * <p><b>남은 일수는 저장하지 않고 매번 셉니다.</b> 저장하면 날짜가 지나도 안 줄어듭니다.
 * 기준일은 안내문을 받은 날이고, <b>「오늘」은 KST</b> 입니다 —
 * 서버 시간대로 세면 하루가 어긋납니다.
 */
@Service
public class NoteService {

    /** 「오늘」의 기준. 서버 시간대가 아닙니다 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CareNoteRepository notes;
    private final CareNoteLineRepository lines;
    private final CareNoteRuleRepository rules;

    public NoteService(CareNoteRepository notes, CareNoteLineRepository lines,
                       CareNoteRuleRepository rules) {
        this.notes = notes;
        this.lines = lines;
        this.rules = rules;
    }

    /**
     * {@code NOW-NOTE-003} 원문 조회.
     *
     * <p><b>기간이 지난 제한도 그대로 돌려줍니다.</b> 이건 원문을 보는 화면이라
     * 「그때 이런 안내를 받았다」가 남아야 합니다. 판정에 쓰는 것은
     * {@link NoteRulePortAdapter} 쪽이고 거기서는 살아 있는 것만 나갑니다.
     *
     * @return 안내문이 없으면 {@link Optional#empty()}
     */
    @Transactional(readOnly = true)
    public Optional<NoteRes> latest(String userId) {
        return notes.findTopByUserIdOrderByReceivedAtDescCreatedAtDesc(userId)
                .map(note -> {
                    List<String> text = lines.findByCareNoteIdOrderBySentNoAsc(note.id())
                            .stream().map(CareNoteLine::text).toList();

                    List<NoteRes.Rule> out = new ArrayList<>();
                    for (CareNoteRule r : rules.findByCareNoteIdOrderBySentNoAsc(note.id())) {
                        out.add(new NoteRes.Rule(r.sentNo(), r.dp(), r.name(),
                                parseKeywords(r.keywords()), r.careItemId()));
                    }
                    return new NoteRes(note.title(), note.fromName(), note.sample(), text, out);
                });
    }

    /**
     * 판정에 넘길 <b>살아 있는 제한만</b>.
     *
     * @return 안내문이 없거나 전부 기간이 지났으면 <b>빈 목록</b>
     */
    @Transactional(readOnly = true)
    public List<NoteRulePort.NoteRule> activeRules(String userId) {
        return notes.findTopByUserIdOrderByReceivedAtDescCreatedAtDesc(userId)
                .map(note -> {
                    long passed = ChronoUnit.DAYS.between(note.receivedAt(), LocalDate.now(KST));
                    List<NoteRulePort.NoteRule> out = new ArrayList<>();
                    for (CareNoteRule r : rules.findByCareNoteIdOrderBySentNoAsc(note.id())) {
                        int left = (int) Math.max(0, r.dp() - passed);
                        // 기간이 지난 것은 안 넘깁니다. 넘기면 지난 제한으로 항목을 계속 막습니다
                        if (left <= 0) continue;
                        out.add(new NoteRulePort.NoteRule(
                                r.sentNo(), r.dp(), r.name(),
                                parseKeywords(r.keywords()), r.careItemId(), left));
                    }
                    return out;
                })
                .orElseGet(List::of);
    }

    /**
     * {@code keywords} 는 JSON 배열 문자열입니다.
     *
     * <p>깨져 있어도 <b>예외를 던지지 않고 빈 목록</b>으로 둡니다 — 키워드 하나 때문에
     * 안내문 화면 전체가 죽으면 안 됩니다.
     */
    private static List<String> parseKeywords(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }
}
