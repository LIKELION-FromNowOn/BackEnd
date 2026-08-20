package com.youin.now.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code care_note_rules} — 안내문 문장에서 뽑아낸 제한 하나.
 *
 * <p><b>모든 제한이 원문 문장 번호를 답니다.</b> 「왜 이걸 하지 말라는 거지」에
 * 원문으로 답할 수 있어야 하기 때문입니다.
 *
 * <p><b>남은 일수는 저장하지 않습니다.</b> {@code daysLeft = max(0, dp − 경과일)} 로
 * 조회할 때마다 셉니다. 저장해 두면 날짜가 지나도 안 줄어듭니다.
 */
@Entity
@Table(name = "care_note_rules")
public class CareNoteRule {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                          // cnr_ + ULID

    @Column(name = "care_note_id", nullable = false)
    private String careNoteId;

    /** 어느 문장에서 나온 제한인지. 응답에서는 {@code sent} 입니다 */
    @Column(name = "sent_no", nullable = false)
    private short sentNo;

    /** 「문지르는 세안」 처럼 사람이 읽는 이름 */
    @Column(name = "name", nullable = false)
    private String name;

    /** <b>JSON 배열 문자열입니다.</b> 꺼내 쓰는 쪽에서 풀어야 합니다 */
    @Column(name = "keywords", nullable = false)
    private String keywords;

    /** 제한 일수 (D+n). 응답에서는 {@code dp} 입니다 */
    @Column(name = "dp", nullable = false)
    private short dp;

    /** 걸리는 관리 항목. <b>{@code NULL} 일 수 있습니다</b> — 항목에 못 붙는 제한도 있습니다 */
    @Column(name = "care_item_id")
    private String careItemId;

    protected CareNoteRule() { }

    public String id()         { return id; }
    public short sentNo()      { return sentNo; }
    public String name()       { return name; }
    public String keywords()   { return keywords; }
    public short dp()          { return dp; }
    public String careItemId() { return careItemId; }
}
