package com.youin.now.today;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code actions} 테이블 — 오늘의 행동 하나.
 *
 * <p><b>{@code rank} 는 MySQL 8 예약어입니다.</b> 백틱으로 감싸지 않으면
 * {@code You have an error in your SQL syntax near 'rank'} 가 나는데 원인을 찾기 어렵습니다.
 *
 * <p><b>{@code status} 는 CHECK 제약이 걸려 있습니다</b> —
 * {@code pending} · {@code running} · {@code done} · {@code rejected}.
 * 다른 값을 넣으면 저장이 터집니다.
 *
 * <p><b>{@code rerollLeft} 와 {@code generatedBy} 는 컬럼이 아닙니다.</b> 응답에서만 계산합니다.
 */
@Entity
@Table(name = "actions")
public class TodayAction {

    /** {@code pending} → {@code running} → {@code done}. 거절은 {@code rejected} */
    public static final String PENDING  = "pending";
    public static final String RUNNING  = "running";
    public static final String DONE     = "done";
    public static final String REJECTED = "rejected";

    @Id
    @Column(name = "id", nullable = false)
    private String id;                      // ac_ + ULID

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** {@code actions.evaluation_id} 는 NOT NULL 외래키입니다 */
    @Column(name = "evaluation_id", nullable = false)
    private String evaluationId;

    /** {@code user_items.id}. 마스터 ID 를 넣으면 외래키가 터집니다 */
    @Column(name = "user_item_id", nullable = false)
    private String userItemId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "duration_sec", nullable = false)
    private int durationSec;

    @Column(name = "status", nullable = false)
    private String status;

    /** ★ MySQL 8 예약어 — 백틱 필수 */
    @Column(name = "`rank`", nullable = false)
    private short rank;

    @Column(name = "total_candidates", nullable = false)
    private short totalCandidates;

    @Column(name = "reroll_count", nullable = false)
    private short rerollCount;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected TodayAction() { }

    public TodayAction(String id, String userId, String evaluationId, String userItemId,
                       String title, int durationSec, short rank, short totalCandidates,
                       OffsetDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.evaluationId = evaluationId;
        this.userItemId = userItemId;
        this.title = title;
        this.durationSec = durationSec;
        this.status = PENDING;
        this.rank = rank;
        this.totalCandidates = totalCandidates;
        this.rerollCount = 0;
        this.expiresAt = expiresAt;
    }

    /** 다시 받기 — 같은 행을 고쳐 씁니다. 하루에 행 하나입니다 */
    public void reroll(String title, int durationSec, String userItemId,
                       short rank, short totalCandidates) {
        this.title = title;
        this.durationSec = durationSec;
        this.userItemId = userItemId;
        this.rank = rank;
        this.totalCandidates = totalCandidates;
        this.status = PENDING;
        this.startedAt = null;
        this.completedAt = null;
        this.rerollCount++;
    }

    public void start(OffsetDateTime at)    { this.status = RUNNING;  this.startedAt = at; }
    public void complete(OffsetDateTime at) { this.status = DONE;     this.completedAt = at; }
    public void reject()                    { this.status = REJECTED; }

    public String id()                  { return id; }
    public String userId()              { return userId; }
    public String evaluationId()        { return evaluationId; }
    public String userItemId()          { return userItemId; }
    public String title()               { return title; }
    public int durationSec()            { return durationSec; }
    public String status()              { return status; }
    public short rank()                 { return rank; }
    public short totalCandidates()      { return totalCandidates; }
    public short rerollCount()          { return rerollCount; }
    public OffsetDateTime startedAt()   { return startedAt; }
    public OffsetDateTime completedAt() { return completedAt; }
    public OffsetDateTime expiresAt()   { return expiresAt; }
    public OffsetDateTime createdAt()   { return createdAt; }
}