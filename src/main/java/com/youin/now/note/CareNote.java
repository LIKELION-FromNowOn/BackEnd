package com.youin.now.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code care_notes} — 클리닉이 준 안내문 한 장.
 *
 * <p><b>실제 문서가 아니라 형식만 재현한 가상 샘플입니다.</b> {@code isSample} 이 {@code true} 면
 * 화면에 그렇게 표시해야 합니다 — 명세 {@code NOW-NOTE-003}.
 *
 * <p>{@code receivedAt} 이 <b>D+n 계산의 기준일</b>입니다. 규칙의 남은 일수를 여기서 셉니다.
 */
@Entity
@Table(name = "care_notes")
public class CareNote {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                          // cn_ + ULID

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "title", nullable = false)
    private String title;

    /** 발신 클리닉. 응답에서는 {@code from} 입니다 */
    @Column(name = "from_name")
    private String fromName;

    @Column(name = "is_sample", nullable = false)
    private boolean sample;

    /** <b>D+n 계산 기준일.</b> 안내문을 받은 날입니다 */
    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    /** 같은 날 두 장을 받았을 때 순서를 가리는 데 씁니다 */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected CareNote() { }

    /**
     * {@code PUT /me/care} 가 만드는 관리 맥락.
     *
     * <p><b>발신 클리닉이 없습니다.</b> 사용자가 직접 적는 것이라 문서가 없습니다 —
     * {@code from_name} 을 {@code NULL} 허용으로 바꾼 이유입니다({@code db/patch_note_tables_v1.sql}).
     *
     * @param receivedAt <b>{@code ago} 를 날짜로 바꾼 값.</b> 절대 날짜를 받지 않습니다 —
     *                   {@code daysLeft} 계산의 기준이라 여기가 틀리면 제한이 어긋납니다
     */
    public CareNote(String id, String userId, String title, LocalDate receivedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.receivedAt = receivedAt;
        this.sample = false;
    }

    public String id()            { return id; }
    public String title()         { return title; }
    public String fromName()      { return fromName; }
    public boolean sample()       { return sample; }
    public LocalDate receivedAt() { return receivedAt; }
}
