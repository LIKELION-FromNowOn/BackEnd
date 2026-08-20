package com.youin.now.footstep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code categories} 읽기 전용. 첫 발자국 응답의 {@code categoryName} 에만 씁니다.
 *
 * <p>마스터 API 3건을 만들 때 {@code master/} 로 옮기고 여기서는 그것을 쓰면 됩니다.
 */
@Entity
@Table(name = "categories")
public class FootstepCategory {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    protected FootstepCategory() {}

    public String getId() { return id; }
    public String getName() { return name; }
}