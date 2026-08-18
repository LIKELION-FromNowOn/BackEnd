package com.youin.now.note;

import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * <b>임시 스텁입니다. 살아 있는 규칙이 없다고 답합니다.</b>
 *
 * <p>진짜 구현이 들어오면 <b>이 파일을 삭제</b>하십시오.
 */
@Primary
@Component
public class NoteRulePortStub implements NoteRulePort {

    @Override
    public List<NoteRule> activeRules(Long userId) {
        return List.of();
    }
}
