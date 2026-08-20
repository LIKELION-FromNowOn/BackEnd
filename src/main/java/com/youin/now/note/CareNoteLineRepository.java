package com.youin.now.note;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareNoteLineRepository
        extends JpaRepository<CareNoteLine, CareNoteLine.Key> {

    /** <b>문장 번호 순서가 곧 원문 순서입니다.</b> 정렬을 빼면 근거가 어긋납니다 */
    List<CareNoteLine> findByCareNoteIdOrderBySentNoAsc(String careNoteId);
}
