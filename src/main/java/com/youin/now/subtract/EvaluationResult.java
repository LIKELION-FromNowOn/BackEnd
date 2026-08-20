package com.youin.now.subtract;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code evaluation_results} 테이블 — 항목 하나의 판정 결과.
 *
 * <p><b>{@code floor} 는 스냅숏입니다.</b> 마스터의 하한선이 나중에 바뀌어도
 * 그날 어떤 기준으로 판정했는지가 남아야 하기 때문에 값을 복사해 둡니다.
 *
 * <p><b>{@code floorApplied} 는 서버가 LLM 판정을 되돌렸다는 표시입니다.</b>
 * ⑦단계에서 하한선을 넘는 판정이 나오면 폐기하고 이 값을 true 로 둡니다.
 *
 * <p><b>{@code excluded} 는 되돌릴 수 없습니다.</b> 클리닉 안내는 앱이 판단하지 않는
 * 영역이라 되돌리기가 있으면 앱이 그것을 덮어쓰는 셈이 됩니다 ({@code CANNOT_REVERT_EXCLUDED}).
 */
@Entity
@Table(name = "evaluation_results")
public class EvaluationResult {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                      // er_ + ULID

    @Column(name = "evaluation_id", nullable = false)
    private String evaluationId;

    @Column(name = "user_item_id", nullable = false)
    private String userItemId;

    /** {@code keep} · {@code simplify} · {@code reduce} · {@code skip} · {@code excluded} */
    @Column(name = "verdict", nullable = false)
    private String verdict;

    /** 근거 문장. 한두 문장. <b>길이가 명세에 없어 500 으로 잡았습니다</b> ({@code REQUESTS.md} #9) */
    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "evidence_level", nullable = false)
    private String evidenceLevel;

    /** 판정 시점의 하한선 스냅숏 */
    @Column(name = "floor", nullable = false)
    private String floor;

    /** 서버가 LLM 판정을 되돌렸으면 true */
    @Column(name = "floor_applied", nullable = false)
    private boolean floorApplied;

    /** 사용자가 되돌렸으면 true. <b>{@code excluded} 는 되돌릴 수 없습니다</b> */
    @Column(name = "reverted", nullable = false)
    private boolean reverted = false;

    /** {@code medical} 또는 {@code clinicNote}. {@code excluded} 일 때만 */
    @Column(name = "excluded_by")
    private String excludedBy;

    /** 안내문 원문 문장 번호. {@code clinicNote} 일 때만 */
    @Column(name = "note_sent")
    private Short noteSent;

    /** 남은 제한 일수. {@code clinicNote} 일 때만. <b>저장값이지만 조회 시 다시 계산합니다</b> */
    @Column(name = "days_left")
    private Short daysLeft;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected EvaluationResult() {}

    public EvaluationResult(String id, String evaluationId, String userItemId,
                            String verdict, String reason, String evidenceLevel,
                            String floor, boolean floorApplied,
                            String excludedBy, Short noteSent, Short daysLeft,
                            boolean reverted) {
        this.id = id;
        this.evaluationId = evaluationId;
        this.userItemId = userItemId;
        this.verdict = verdict;
        this.reason = reason;
        this.evidenceLevel = evidenceLevel;
        this.floor = floor;
        this.floorApplied = floorApplied;
        this.excludedBy = excludedBy;
        this.noteSent = noteSent;
        this.daysLeft = daysLeft;
        this.reverted = reverted;
    }

    /**
     * 사용자가 되돌렸습니다. <b>부르기 전에 {@code excluded} 인지 확인하십시오.</b>
     *
     * <p>{@code verdict} 도 함께 {@code keep} 이 됩니다 — 명세서 {@code NOW-SUB-003} 이
     * 「되돌린 뒤의 판정. <b>항상 keep</b>」이고 {@code summary} 는 「<b>갱신된</b> 건수」입니다.
     * 표시만 바꾸고 값을 두면 {@code GET /subtract/result} 가 옛 판정을 계속 보여 줍니다.
     */
    public void revert() {
        this.reverted = true;
        this.verdict = SubtractVerdict.KEEP.code();
    }

    public String id()            { return id; }
    public String evaluationId()  { return evaluationId; }
    public String userItemId()    { return userItemId; }
    public String verdict()       { return verdict; }
    public String reason()        { return reason; }
    public String evidenceLevel() { return evidenceLevel; }
    public String floor()         { return floor; }
    public boolean floorApplied() { return floorApplied; }
    public boolean reverted()     { return reverted; }
    public String excludedBy()    { return excludedBy; }
    public Short noteSent()       { return noteSent; }
    public Short daysLeft()       { return daysLeft; }
}
