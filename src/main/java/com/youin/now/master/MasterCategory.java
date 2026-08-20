package com.youin.now.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code categories} 테이블 — 카테고리 7건. <b>읽기 전용 마스터입니다.</b>
 *
 * <p>앱이 이 테이블에 쓰지 않습니다. 값은 {@code db/seed_master_v1.sql} 이 넣습니다.
 * 그래서 수정자도 생성자도 두지 않았습니다.
 */
@Entity
@Table(name = "categories")
public class MasterCategory {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected MasterCategory() { }

    public String id()       { return id; }
    public String name()     { return name; }
    public short sortOrder() { return sortOrder; }
}