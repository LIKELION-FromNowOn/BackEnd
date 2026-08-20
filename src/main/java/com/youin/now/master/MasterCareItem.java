package com.youin.now.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * {@code care_items} 테이블 — 관리 항목 32건. <b>읽기 전용 마스터입니다.</b>
 *
 * <p><b>{@code core} 와 {@code base} 는 {@code DECIMAL(3,1)} 입니다</b> (2026-08-20 변경).
 * 판정 점수식에 직접 들어가므로 {@code double} 로 받으면 소수 오차가 생깁니다.
 *
 * <p><b>{@code floor} 와 {@code evidenceLevel} 은 숫자가 아니라 문자열입니다.</b>
 * CHECK 제약으로 값이 고정돼 있습니다.
 */
@Entity
@Table(name = "care_items")
public class MasterCareItem {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    /** essential · recommended · optional · excluded */
    @Column(name = "floor", nullable = false)
    private String floor;

    /** high · medium · low · none */
    @Column(name = "evidence_level", nullable = false)
    private String evidenceLevel;

    @Column(name = "core", nullable = false, precision = 3, scale = 1)
    private BigDecimal core;

    @Column(name = "base", nullable = false, precision = 3, scale = 1)
    private BigDecimal base;

    @Column(name = "minutes", nullable = false)
    private short minutes;

    /** 클리닉·처방약처럼 앱이 판정하지 않는 항목은 false. 32건 중 10건이 true */
    @Column(name = "frequency_editable", nullable = false)
    private boolean frequencyEditable;

    /** weekly_1 · weekly_2 · weekly_3 · weekly_4plus · daily. 없으면 null */
    @Column(name = "default_frequency")
    private String defaultFrequency;

    protected MasterCareItem() { }

    public String id()                 { return id; }
    public String categoryId()         { return categoryId; }
    public String name()               { return name; }
    public String floor()              { return floor; }
    public String evidenceLevel()      { return evidenceLevel; }
    public BigDecimal core()           { return core; }
    public BigDecimal base()           { return base; }
    public short minutes()             { return minutes; }
    public boolean frequencyEditable() { return frequencyEditable; }
    public String defaultFrequency()   { return defaultFrequency; }
}