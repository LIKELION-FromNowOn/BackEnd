package com.youin.now.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/** 상태 전환 제안과 사용자 응답. accepted가 null이면 아직 응답하지 않은 제안입니다. */
@Entity
@Table(name = "state_transitions")
public class CheckinStateTransition {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "from_state", nullable = false)
    private String fromState;

    @Column(name = "to_state", nullable = false)
    private String toState;

    @Column(name = "signal_score", nullable = false)
    private short signalScore;

    @Column(name = "accepted")
    private Boolean accepted;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected CheckinStateTransition() {}

    public CheckinStateTransition(String id, String userId, String fromState, String toState, short signalScore) {
        this.id = id;
        this.userId = userId;
        this.fromState = fromState;
        this.toState = toState;
        this.signalScore = signalScore;
    }

    public void respond(boolean accepted, OffsetDateTime respondedAt) {
        this.accepted = accepted;
        this.respondedAt = respondedAt;
    }

    public String fromState() { return fromState; }
    public String toState() { return toState; }
    public Boolean accepted() { return accepted; }
    public OffsetDateTime respondedAt() { return respondedAt; }
}
