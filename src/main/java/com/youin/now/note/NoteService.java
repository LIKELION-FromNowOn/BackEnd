package com.youin.now.note;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.safety.SafetyPort;
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

    /** 자유 텍스트는 저장 전에 이걸 통과해야 합니다. safety/ 는 제 것이라 창구 없이 씁니다 */
    private final SafetyPort safety;

    public NoteService(CareNoteRepository notes, CareNoteLineRepository lines,
                       CareNoteRuleRepository rules,
                       SafetyPort safety) {
        this.notes = notes;
        this.lines = lines;
        this.rules = rules;
        this.safety = safety;
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
    /**
     * 안내문이 등록되어 있는가. <b>제한이 전부 풀렸어도 {@code true} 입니다.</b>
     *
     * <p>{@code activeRules} 가 빈 목록인 것과 구분해야 합니다 —
     * 안내문은 있는데 기간이 다 지난 경우가 있고, 그때도 원문은 볼 수 있어야 합니다.
     */
    /**
     * {@code NOW-NOTE-001} 관리 맥락 조회.
     *
     * <p><b>등록된 것이 없으면 빈 값입니다. 404 를 내지 않습니다</b> — 명세서 규칙입니다.
     *
     * <p>{@code daysLeft} 가 0 이 된 주의사항은 <b>담지 않습니다.</b> 지난 제한을 계속 보여 주면
     * 사용자가 하지 않아도 될 것을 피하게 됩니다.
     */
    @Transactional(readOnly = true)
    public CareRes careContext(String userId) {
        return notes.findTopByUserIdOrderByReceivedAtDescCreatedAtDesc(userId)
                .map(note -> {
                    long passed = ChronoUnit.DAYS.between(note.receivedAt(), LocalDate.now(KST));
                    List<CareRes.Caution> out = new ArrayList<>();
                    for (CareNoteRule r : rules.findByCareNoteIdOrderBySentNoAsc(note.id())) {
                        // dp 가 0 이면 기간을 모르는 것입니다. 자동 만료시키지 않습니다
                        Integer dp = r.dp() == 0 ? null : (int) r.dp();
                        Integer left = dp == null ? null : (int) Math.max(0, dp - passed);
                        if (left != null && left <= 0) continue;
                        out.add(new CareRes.Caution(
                                r.careItemId(),
                                r.cautionText() != null ? r.cautionText() : r.name(),
                                r.sentNo(), dp, left, parseKeywords(r.keywords())));
                    }
                    return new CareRes(note.title(), (int) Math.max(0, passed), out, true, null);
                })
                .orElseGet(CareRes::empty);
    }

    /**
     * {@code NOW-NOTE-002} 관리 맥락 저장. <b>통째로 갈아 끼웁니다.</b>
     *
     * <p>순서가 강제됩니다 — {@code care_note_rules} 의 외래키가
     * {@code (care_note_id, sent_no)} 로 {@code care_note_lines} 를 가리킵니다.
     * <b>문장을 먼저 넣어야 주의사항을 넣을 수 있습니다.</b>
     *
     * <p>자유 텍스트는 <b>저장 전에 위기 신호 검사를 통과해야 합니다</b> —
     * {@code schema_v63.sql} 주석과 명세서가 둘 다 요구합니다.
     */
    @Transactional
    public CareRes saveCare(String userId, CareReq req) {
        List<String> lines = req.noteLines() == null ? List.of() : req.noteLines();
        List<CareReq.Caution> cautions = req.cautions() == null ? List.of() : req.cautions();

        // ① 위기 신호 검사 — 저장 전에 전부
        check(req.lastType());
        for (String line : lines) check(line);
        for (CareReq.Caution c : cautions) check(c.text());

        // ② sent 가 가리키는 문장이 있어야 합니다. 없으면 외래키에서 터집니다
        for (CareReq.Caution c : cautions) {
            if (c.sent() > lines.size()) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        c.sent() + "번째 문장이 noteLines 에 없습니다");
            }
        }

        // ③ 통째로 갈아 끼웁니다. lines · rules 는 CASCADE 로 따라 지워집니다
        notes.deleteByUserId(userId);

        String noteId = Ids.of("cn");
        notes.save(new CareNote(noteId, userId, req.lastType(),
                LocalDate.now(KST).minusDays(req.ago())));

        short no = 1;
        for (String line : lines) this.lines.save(new CareNoteLine(noteId, no++, line));

        for (CareReq.Caution c : cautions) {
            rules.save(new CareNoteRule(Ids.of("cnr"), noteId,
                    c.sent().shortValue(), c.text(), toJson(c.keywords()),
                    c.dp() == null ? 0 : c.dp().shortValue(), c.itemId()));
        }

        CareRes saved = careContext(userId);
        return new CareRes(saved.lastType(), saved.ago(), saved.cautions(),
                saved.hasNote(), cautions.size());
    }

    /**
     * 매칭용 낱말을 JSON 배열 문자열로. <b>없으면 {@code null} 입니다.</b>
     *
     * <p>이 값이 비면 그 주의사항은 <b>아무 데도 안 걸립니다</b> —
     * 케어 코치가 못 찾고({@code CoachService}), 예정의 충돌 표시도 안 뜹니다
     * ({@code PlanService.conflictOf} 가 {@code keywords} 로 맞춰 봅니다).
     * 2026-08-20 에 요청 필드로 올렸습니다 — 서버가 문장에서 뽑아내면 지어내는 것이 됩니다.
     */
    private static String toJson(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(keywords);
        } catch (Exception e) {
            return null;
        }
    }

    /** 자유 텍스트는 저장 전에 반드시 통과해야 합니다. LLM 이 없는 자리라 순서 문제는 없습니다 */
    private void check(String text) {
        if (text == null || text.isBlank()) return;
        if (safety.check(text, SafetyPort.Source.NOTE).blocked()) {
            throw new ApiException(ErrorCode.TEXT_REJECTED);
        }
    }

    /**
     * 홈의 {@code care} 블록. <b>{@link #careContext} 를 홈 모양으로 줄인 것</b>입니다.
     *
     * <p>홈은 {@code dp} 와 {@code keywords} 를 안 씁니다 — 명세 {@code NOW-HOME-001} 의
     * {@code care.cautions[]} 는 {@code itemId} · {@code text} · {@code sent} · {@code daysLeft} 넷입니다.
     * 안 쓰는 값을 실어 보내면 홈 응답이 커지기만 합니다.
     */
    @Transactional(readOnly = true)
    public NoteRulePort.CareContext careForHome(String userId) {
        CareRes c = careContext(userId);
        List<NoteRulePort.Caution> out = new ArrayList<>();
        for (CareRes.Caution x : c.cautions()) {
            out.add(new NoteRulePort.Caution(x.itemId(), x.text(), x.sent(), x.daysLeft()));
        }
        return new NoteRulePort.CareContext(c.lastType(), c.ago(), out, c.hasNote());
    }

    @Transactional(readOnly = true)
    public boolean hasNote(String userId) {
        return notes.findTopByUserIdOrderByReceivedAtDescCreatedAtDesc(userId).isPresent();
    }

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
                                r.sentNo(), r.dp(), r.name(), r.cautionText(),
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
