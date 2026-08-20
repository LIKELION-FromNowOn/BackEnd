package com.youin.now.safety;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code safety_checks} — 검사 한 번의 기록.
 *
 * <p><b>★ 원문을 남기지 않습니다.</b> 위기 신호가 담긴 문장을 그대로 보관하면
 * 그 자체가 위험합니다. <b>해시와 걸린 키워드만</b> 남깁니다.
 *
 * <p>{@code user_id} 가 {@code NULL} 을 받습니다 — <b>비회원도 부를 수 있어야 합니다.</b>
 * 게스트 토큰을 받기 전 단계의 입력도 보호해야 해서입니다 (명세 {@code NOW-SAFE-001}).
 */
@Entity
@Table(name = "safety_checks")
public class SafetyCheck {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                              // sc_ + ULID

    /** 비회원이면 {@code null} */
    @Column(name = "user_id")
    private String userId;

    /** {@code custom_item} · {@code custom_signal} · {@code coach} · {@code care_note} · {@code plan} */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "matched", nullable = false)
    private boolean matched;

    /** 걸린 키워드 하나. 여러 개면 첫 번째입니다. 안 걸렸으면 {@code null} */
    @Column(name = "matched_keyword")
    private String matchedKeyword;

    /** <b>원문이 아니라 SHA-256 입니다.</b> 같은 문장이 반복되는지만 볼 수 있습니다 */
    @Column(name = "text_hash", nullable = false)
    private String textHash;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected SafetyCheck() { }

    public SafetyCheck(String id, String userId, String source,
                       boolean matched, String matchedKeyword, String textHash) {
        this.id = id;
        this.userId = userId;
        this.source = source;
        this.matched = matched;
        this.matchedKeyword = matchedKeyword;
        this.textHash = textHash;
    }

    public String id()             { return id; }
    public boolean matched()       { return matched; }
    public String matchedKeyword() { return matchedKeyword; }
}
