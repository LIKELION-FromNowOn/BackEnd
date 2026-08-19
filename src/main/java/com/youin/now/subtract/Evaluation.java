package com.youin.now.subtract;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code evaluations} 테이블 — 판정 한 번.
 *
 * <p><b>체크인 하나에 판정 하나입니다.</b> {@code ux_evaluations_checkin} 이 강제합니다.
 * 같은 체크인으로 다시 판정하면 새로 만들지 않고 기존 것을 고칩니다.
 *
 * <p><b>{@code generatedBy} 가 발표에서 쓰입니다.</b> 「AI 가 진짜 도는지」를 이 필드로 보여 줍니다.
 * {@code llm} 이면 ⑥단계가 실제로 돌았고, {@code fallback} 이면 미리 정해 둔 문장을 썼다는 뜻입니다.
 */
@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                      // ev_ + ULID

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** 체크인 하나에 판정 하나. 유니크 제약이 걸려 있습니다 */
    @Column(name = "checkin_id", nullable = false)
    private String checkinId;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "judge_strength", nullable = false)
    private String judgeStrength;

    /** {@code llm} 또는 {@code fallback}. <b>발표에서 이 필드를 보여 줍니다</b> */
    @Column(name = "generated_by", nullable = false)
    private String generatedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected Evaluation() {}

    public Evaluation(String id, String userId, String checkinId,
                      String state, String judgeStrength, String generatedBy) {
        this.id = id;
        this.userId = userId;
        this.checkinId = checkinId;
        this.state = state;
        this.judgeStrength = judgeStrength;
        this.generatedBy = generatedBy;
    }

    /** 같은 체크인으로 다시 판정한 경우. */
    public void update(String state, String judgeStrength, String generatedBy) {
        this.state = state;
        this.judgeStrength = judgeStrength;
        this.generatedBy = generatedBy;
    }

    public String id()            { return id; }
    public String userId()        { return userId; }
    public String checkinId()     { return checkinId; }
    public String state()         { return state; }
    public String judgeStrength() { return judgeStrength; }
    public String generatedBy()   { return generatedBy; }
    public OffsetDateTime createdAt() { return createdAt; }
}
