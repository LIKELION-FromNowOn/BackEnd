package com.youin.now.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code checkins} 테이블 — 오늘 상태 체크 한 건.
 *
 * <p><b>하루에 한 건입니다.</b> {@code ux_checkins_user_date} 가 강제합니다.
 * 같은 날 다시 제출하면 새로 만들지 않고 기존 것을 고칩니다.
 *
 * <p><b>날짜 경계는 KST 자정입니다.</b> {@code check_date} 는 {@code DATE} 라 시간대를
 * 저장하지 않으므로, 넣을 때 {@code Asia/Seoul} 로 계산해 넣어야 합니다.
 * 서버 시간대가 다르면 「오늘」이 하루 어긋납니다.
 *
 * <p><b>다른 패키지는 이 엔티티를 받지 않습니다.</b> {@link CheckinPort} 로만 나갑니다.
 */
@Entity
@Table(name = "checkins")
public class Checkin {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                          // ck_ + ULID

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** KST 기준 날짜. <b>서버 시간대로 계산하지 마십시오.</b> */
    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    /** {@code energetic} · {@code normal} · {@code low} · {@code drained} · {@code unknown} */
    @Column(name = "state", nullable = false)
    private String state;

    /** {@code low} · {@code medium} · {@code high} · {@code max}. 상태에서 파생됩니다 */
    @Column(name = "judge_strength", nullable = false)
    private String judgeStrength;

    /** 고른 징후의 가중치 합. <b>5를 넘으면 상태 전환을 제안합니다</b> (합계 상한 25) */
    @Column(name = "signal_score", nullable = false)
    private short signalScore;

    /** <b>{@code insertable = false} 라 값은 DB 가 채웁니다.</b> */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected Checkin() {}                      // JPA 용

    public Checkin(String id, String userId, LocalDate checkDate,
                   String state, String judgeStrength, short signalScore) {
        this.id = id;
        this.userId = userId;
        this.checkDate = checkDate;
        this.state = state;
        this.judgeStrength = judgeStrength;
        this.signalScore = signalScore;
    }

    /** 같은 날 다시 제출한 경우. 새 행을 만들지 않고 덮어씁니다. */
    public void update(String state, String judgeStrength, short signalScore) {
        this.state = state;
        this.judgeStrength = judgeStrength;
        this.signalScore = signalScore;
    }

    /** 전환 제안을 수락한 경우에만 상태를 한 단계 바꿉니다. */
    public void transitionTo(String state, String judgeStrength) {
        this.state = state;
        this.judgeStrength = judgeStrength;
    }

    public String id()             { return id; }
    public String userId()         { return userId; }
    public LocalDate checkDate()   { return checkDate; }
    public String state()          { return state; }
    public String judgeStrength()  { return judgeStrength; }
    public short signalScore()     { return signalScore; }
    public OffsetDateTime createdAt() { return createdAt; }
}
