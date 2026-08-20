package com.youin.now.note;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code care_notes} 조회. <b>읽기만 합니다</b> — 저장은 {@code care/}(김민정 님) 몫입니다. */
public interface CareNoteRepository extends JpaRepository<CareNote, String> {

    /** 가장 최근에 받은 안내문 한 장 */
    Optional<CareNote> findTopByUserIdOrderByReceivedAtDescCreatedAtDesc(String userId);

    /**
     * {@code PUT /me/care} 는 통째로 갈아 끼웁니다.
     *
     * <p><b>{@code care_note_lines} · {@code care_note_rules} 가 따라 지워집니다</b> —
     * 2026-08-20 에 {@code fk_care_note_rules_line} 에 {@code ON DELETE CASCADE} 를 걸었습니다.
     * 그 전에는 여기서 {@code ERROR 1451} 이 났습니다({@code REQUESTS #21}).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CareNote n where n.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
