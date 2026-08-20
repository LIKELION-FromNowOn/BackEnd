package com.youin.now.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * {@code care_note_lines} — 안내문의 문장 하나.
 *
 * <p><b>{@code sentNo} 가 이 앱 신뢰 구조의 핵심입니다.</b> 코치가 「안내문 4번째 문장에」라고
 * 근거를 가리킬 수 있는 이유가 이 번호입니다. <b>1부터 셉니다.</b>
 *
 * <p>키가 둘입니다 ({@code care_note_id} + {@code sent_no}). 그래서 {@link Key} 를 씁니다.
 */
@Entity
@Table(name = "care_note_lines")
@IdClass(CareNoteLine.Key.class)
public class CareNoteLine {

    @Id
    @Column(name = "care_note_id", nullable = false)
    private String careNoteId;

    @Id
    @Column(name = "sent_no", nullable = false)
    private short sentNo;

    /** <b>원문 그대로입니다. 요약하지 않습니다</b> — 명세 규칙 */
    @Column(name = "text", nullable = false)
    private String text;

    protected CareNoteLine() { }

    public short sentNo() { return sentNo; }
    public String text()  { return text; }

    /** 복합 키. JPA 가 요구하는 모양입니다 */
    public static class Key implements Serializable {
        private String careNoteId;
        private short sentNo;

        public Key() { }
        public Key(String careNoteId, short sentNo) {
            this.careNoteId = careNoteId;
            this.sentNo = sentNo;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return sentNo == k.sentNo && Objects.equals(careNoteId, k.careNoteId);
        }
        @Override public int hashCode() { return Objects.hash(careNoteId, sentNo); }
    }
}
