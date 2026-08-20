package com.youin.now.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code users} 테이블.
 *
 * <p><b>클래스 이름이 {@code User} 가 아니라 {@code AuthUser} 인 이유</b> —
 * {@code User} 로 시작하는 클래스는 {@code auth/} 예약어이고, 다른 패키지에 같은 이름이 생기면
 * Git 충돌이 아니라 <b>스프링 부팅이 실패</b>합니다({@code ConflictingBeanDefinitionException}).
 * 접두어를 붙여 두면 그 사고가 아예 안 납니다.
 *
 * <p><b>게스트도 이 테이블에 넣습니다.</b> 회원 전환 시 {@code isGuest} 만 false 로 바꾸면
 * 그때까지 쌓은 기록이 그대로 남습니다. 게스트 전용 테이블을 두면 전환할 때 전부 옮겨야 합니다.
 *
 * <p><b>다른 패키지는 이 엔티티를 받지 않습니다.</b> {@code Long}(여기서는 {@code String}) 사용자 번호만
 * 주고받습니다. {@code @ManyToOne AuthUser} 를 쓰기 시작하면 패키지 14개가 한 덩어리가 됩니다.
 */
@Entity
@Table(name = "users")
public class AuthUser {

    @Id
    @Column(name = "id", nullable = false)
    private String id;                              // us_ + ULID

    @Column(name = "email")
    private String email;                           // 게스트는 null

    @Column(name = "password_hash")
    private String passwordHash;                    // 게스트는 null

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "is_guest", nullable = false)
    private boolean isGuest = true;

    @Column(name = "has_seen_onboarding", nullable = false)
    private boolean hasSeenOnboarding = false;

    @Column(name = "recommendation_paused", nullable = false)
    private boolean recommendationPaused = false;

    @Column(name = "paused_until")
    private LocalDate pausedUntil;                  // null 이면 오늘 하루만

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /**
     * <b>{@code insertable = false} 라 값은 DB 가 채웁니다.</b>
     *
     * <p>{@code @ColumnDefault} 가 없으면 로컬 H2 에서 게스트 발급이 깨집니다 —
     * {@code ddl-auto: create-drop} 이 <b>엔티티에서</b> DDL 을 만드는데,
     * {@code insertable = false} 만으로는 기본값이 생기지 않아
     * {@code NULL not allowed for column "CREATED_AT"} 가 납니다 (2026-08-18 확인).
     * 실서버 스키마에는 {@code DEFAULT} 가 이미 있어 드러나지 않던 문제입니다.
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("now()")
    private OffsetDateTime createdAt;               // DB 기본값 now()

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;               // 물리 삭제하지 않습니다

    protected AuthUser() {}                          // JPA 용

    public static AuthUser guest(String id) {
        AuthUser u = new AuthUser();
        u.id = id;
        u.isGuest = true;
        u.lastLoginAt = OffsetDateTime.now();
        return u;
    }

    public static AuthUser member(String id, String email, String nickname, String passwordHash) {
        AuthUser u = new AuthUser();
        u.id = id;
        u.email = email;
        u.nickname = nickname;
        u.passwordHash = passwordHash;
        u.isGuest = false;
        u.lastLoginAt = OffsetDateTime.now();
        return u;
    }

    public String id()                   { return id; }
    public String email()                { return email; }
    public String passwordHash()         { return passwordHash; }
    public String nickname()             { return nickname; }
    public boolean isGuest()             { return isGuest; }
    public boolean hasSeenOnboarding()   { return hasSeenOnboarding; }
    public boolean recommendationPaused(){ return recommendationPaused; }
    public LocalDate pausedUntil()       { return pausedUntil; }
    public OffsetDateTime deletedAt()    { return deletedAt; }

    public void touchLogin()             { this.lastLoginAt = OffsetDateTime.now(); }
    public void markOnboardingSeen()     { this.hasSeenOnboarding = true; }

    /** 회원 전환 — 행을 옮기지 않고 표시만 바꿉니다. */
    public void promote(String email, String nickname, String passwordHash) {
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.isGuest = false;
    }

    public void updateNickname(String nickname) { this.nickname = nickname; }
    public void updateEmail(String email)       { this.email = email; }
}
