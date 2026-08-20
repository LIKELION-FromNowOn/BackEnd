package com.youin.now.care;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * {@code plans} — 예정 하나.
 *
 * <p><b>{@code id} 는 자동 증가가 아닙니다.</b> 저장 전에 {@code Ids.of("pl")} 로 만들어 넣습니다.
 *
 * <p>{@code updated_at} 컬럼이 없습니다. <b>수정 API 가 없는 것과 맞습니다</b> —
 * 조회·추가·삭제뿐입니다.
 *
 * <p><b>{@code title} 은 자유 입력입니다.</b> 저장 전에 {@code SafetyPort} 를 통과해야 합니다
 * (스키마 주석 「위기 신호 검사 통과 필수」).
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                      // pl_ + ULID

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Plan() { }

    public Plan(String id, String userId, String title, LocalDate planDate) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.planDate = planDate;
    }

    public String id()          { return id; }
    public String userId()      { return userId; }
    public String title()       { return title; }
    public LocalDate planDate() { return planDate; }
}