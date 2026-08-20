package com.youin.now.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code signals} 테이블 — 이상 징후 14건. <b>읽기 전용 마스터입니다.</b>
 *
 * <p><b>앱이 이 테이블에 쓰지 않습니다.</b> 값은 {@code db/seed_master_v1.sql} 이 넣습니다.
 * 그래서 수정자(setter)도 생성자도 두지 않았습니다 — 하이버네이트가 쓰는 기본 생성자만 있습니다.
 *
 * <hr>
 *
 * <p>⚠️ <b>{@code master/} 는 김민정 님 폴더입니다</b>({@code docs/03-packages.md:16}).
 * 2026-08-20 에 송원석이 <b>이 파일 셋만</b> 넣었습니다 —
 * {@code MasterSignal} · {@code MasterSignalRepository} · {@code SignalWeightAdapter}.
 *
 * <p><b>마스터 API 3건({@code NOW-MASTER-001~003})은 손대지 않았습니다.</b> 김민정 님 몫 그대로입니다.
 *
 * <p><b>왜 넣었나</b> — {@code POST /checkins} 가 실서버에 살아 있는데 징후 가중치를
 * 못 읽어 {@code signalScore} 가 항상 0 이었습니다. 임계값 5 를 못 넘으니
 * <b>상태 전환 제안이 한 번도 안 떴습니다.</b> 프론트가 그 화면을 만들 수 없었습니다.
 *
 * <p><b>이름이 겹치지 않게 {@code Master} 접두어를 붙였습니다.</b> 김민정 님이
 * {@code Signal} 을 만드셔도 부딪히지 않습니다. 다만 {@link SignalWeightAdapter} 와 같은
 * 일을 하는 {@code @Component} 를 또 만드시면 <b>빈이 둘이라 앱이 안 뜹니다.</b>
 * 그때는 이 셋을 지우십시오.
 */
@Entity
@Table(name = "signals")
public class MasterSignal {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                        // sig_01 … sig_14

    /** 피부 · 수면 · 마음 · 관계 · 생활 */
    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "name", nullable = false)
    private String name;

    /** 14건을 다 더하면 25 입니다. 임계값 5 는 이 합 위에서 정해졌습니다 */
    @Column(name = "weight", nullable = false)
    private short weight;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected MasterSignal() { }

    public String id()        { return id; }
    public String groupName() { return groupName; }
    public String name()      { return name; }
    public short weight()     { return weight; }
    public short sortOrder()  { return sortOrder; }
}
