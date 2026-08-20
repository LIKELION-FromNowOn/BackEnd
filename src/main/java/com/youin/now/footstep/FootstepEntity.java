package com.youin.now.footstep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "footsteps")
public class FootstepEntity {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "category_id", length = 32, nullable = false)
    private String categoryId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    // 익명 프로필 —「20대 직장인」 같은 것
    @Column(name = "who", length = 100, nullable = false)
    private String who;

    @Column(name = "situation", columnDefinition = "TEXT", nullable = false)
    private String situation;

    @Column(name = "first_step", columnDefinition = "TEXT", nullable = false)
    private String firstStep;

    // MySQL json 컬럼. 문자열로 받아 두고 Res 에서 진짜 JSON 으로 바꿉니다
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_steps", nullable = false)
    private String nextSteps;

    @Column(name = "quote", columnDefinition = "TEXT", nullable = false)
    private String quote;

    @Column(name = "is_onboarding", nullable = false)
    private Boolean isOnboarding;

    protected FootstepEntity() {}

    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getWho() { return who; }
    public String getSituation() { return situation; }
    public String getFirstStep() { return firstStep; }
    public String getNextSteps() { return nextSteps; }
    public String getQuote() { return quote; }
    public Boolean getIsOnboarding() { return isOnboarding; }
}