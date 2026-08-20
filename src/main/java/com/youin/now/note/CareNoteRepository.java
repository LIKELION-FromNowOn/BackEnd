package com.youin.now.note;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code care_notes} 조회. <b>읽기만 합니다</b> — 저장은 {@code care/}(김민정 님) 몫입니다. */
public interface CareNoteRepository extends JpaRepository<CareNote, String> {

    /** 가장 최근에 받은 안내문 한 장 */
    Optional<CareNote> findTopByUserIdOrderByReceivedAtDescCreatedAtDesc(String userId);
}
