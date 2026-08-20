package com.youin.now.note;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link NoteRulePort} 실제 구현.
 *
 * <p><b>2026-08-20 {@code NoteRulePortStub} 을 지우고 이것으로 바꿨습니다.</b>
 * 스텁이 빈 목록을 돌려줘서 <b>클리닉 제한이 판정에 한 번도 안 걸렸습니다.</b>
 * 「클리닉 안내가 생활 제안보다 앞선다」가 이 앱의 차별점인데 코드에서 죽어 있었습니다.
 *
 * <p>알맹이는 {@link NoteService#activeRules} 에 있습니다. 이 클래스는 창구일 뿐입니다.
 */
@Component
public class NoteRulePortAdapter implements NoteRulePort {

    private final NoteService noteService;

    public NoteRulePortAdapter(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public List<NoteRule> activeRules(String userId) {
        return noteService.activeRules(userId);
    }

    @Override
    public boolean hasNote(String userId) {
        return noteService.hasNote(userId);
    }

    @Override
    public CareContext careForHome(String userId) {
        return noteService.careForHome(userId);
    }
}
