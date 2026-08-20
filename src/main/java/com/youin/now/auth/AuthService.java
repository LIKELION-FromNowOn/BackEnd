package com.youin.now.auth;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.checkin.CheckinPort;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 게스트 발급과 토큰 검증.
 *
 * <p><b>게스트도 기능 제한이 없습니다.</b> 진입 장벽을 낮추기 위한 결정입니다(명세서 {@code NOW-AUTH-001}).
 */
@Service
public class AuthService {

    /** 날짜 경계가 KST 자정이라 만료 시각도 KST 로 내려 줍니다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final AuthUserRepository users;
    private final AuthTokenProvider tokens;
    private final CheckinPort checkins;

    public AuthService(AuthUserRepository users,
                       CheckinPort checkins,
                       @Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.valid-seconds:2592000}") long validSeconds) {
        this.users = users;
        this.checkins = checkins;
        this.tokens = new AuthTokenProvider(secret, validSeconds);
    }

    /** 게스트 한 명을 만들고 토큰을 발급합니다. */
    @Transactional
    public AuthGuestRes issueGuest() {
        AuthUser user = users.save(AuthUser.guest(Ids.user()));
        AuthTokenProvider.Issued issued = tokens.issue(user.id());

        String expiresAt = OffsetDateTime.ofInstant(issued.expiresAt(), KST)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return new AuthGuestRes(issued.token(), "guest", expiresAt);
    }

    /** 이메일·비밀번호 회원을 만들거나, 유효한 게스트 토큰의 같은 사용자를 회원으로 전환합니다. */
    @Transactional
    public AuthSignupRes signup(AuthSignupReq req) {
        if (!validEmail(req.email()) || !validPassword(req.password()) || !validNickname(req.nickname())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "이메일 · 비밀번호 · 닉네임을 확인해 주세요");
        }
        if (users.findByEmailAndDeletedAtIsNull(req.email()).isPresent()) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        AuthUser guest = guestFrom(req.guestToken());
        AuthSignupRes.Migrated migrated;
        AuthUser member;
        if (guest == null) {
            member = AuthUser.member(Ids.user(), req.email(), req.nickname(), ENCODER.encode(req.password()));
            migrated = new AuthSignupRes.Migrated(0, 0, 0);
        } else {
            AuthUserRepository.MigrationCounts counts = users.countMigratedData(guest.id());
            migrated = new AuthSignupRes.Migrated(counts.getItems(), counts.getCheckins(), counts.getLogs());
            guest.promote(req.email(), req.nickname(), ENCODER.encode(req.password()));
            member = guest;
        }
        try {
            // flush까지 수행해야 DB 제약 위반을 가입 응답의 명세 문구로 바꿀 수 있습니다.
            users.saveAndFlush(member);
        } catch (RuntimeException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "계정 생성에 실패했습니다");
        }
        return new AuthSignupRes(tokens.issue(member.id()).token(), "member", migrated);
    }

    /** 이메일 존재 여부와 비밀번호 불일치를 같은 401로 처리해 계정 열거를 막습니다. */
    @Transactional
    public AuthLoginRes login(AuthLoginReq req) {
        if (!validEmail(req.email()) || !validPassword(req.password())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "이메일 형식이 올바르지 않습니다");
        }
        AuthUser user = users.findByEmailAndDeletedAtIsNull(req.email()).orElse(null);
        if (user == null || user.isGuest() || user.passwordHash() == null || !ENCODER.matches(req.password(), user.passwordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.touchLogin();
        return new AuthLoginRes(tokens.issue(user.id()).token(), "member", user.nickname());
    }

    @Transactional(readOnly = true)
    public AuthMeRes me(String userId) {
        AuthUser user = users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        var latest = checkins.latest(userId);
        return new AuthMeRes(
                user.id(), user.isGuest() ? "guest" : "member", user.nickname(), user.email(),
                latest.map(CheckinPort.LatestCheckin::state).orElse("normal"),
                user.recommendationPaused(), users.countActiveItems(userId), latest.isPresent());
    }

    /**
     * {@code PATCH /me/password} 비밀번호 변경.
     *
     * <p><b>지금 비밀번호를 다시 확인합니다.</b> 토큰만으로 바꾸게 하면 토큰이 새는 순간
     * 계정을 뺏깁니다. 로그인과 같은 {@code 401 INVALID_CREDENTIALS} 를 씁니다 —
     * 「비밀번호가 틀렸다」와 「계정이 없다」를 가르지 않는 것과 같은 이유입니다.
     */
    @Transactional
    public AuthPasswordRes changePassword(String userId, AuthPasswordReq req) {
        AuthUser user = users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.isGuest()) throw new ApiException(ErrorCode.GUEST_FORBIDDEN);

        requireCurrentPassword(user, req.currentPassword());

        if (!validPassword(req.newPassword())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "비밀번호는 8~64자입니다");
        }
        user.updatePassword(ENCODER.encode(req.newPassword()));
        return new AuthPasswordRes(true);
    }

    /**
     * {@code DELETE /me} 회원 탈퇴.
     *
     * <p><b>행을 지우지 않고 {@code deleted_at} 만 찍습니다(2026-08-21 송원석 결정).</b>
     * 조회가 전부 {@code deletedAtIsNull} 로 걸러지고 있어서 그 순간부터 없는 사람이 됩니다.
     * 항목·판정·기록은 다른 표가 이 행을 외래키로 참조하고 있어 지우면 같이 무너집니다.
     *
     * <p>되돌릴 수 없는 동작이라 비밀번호를 다시 받습니다.
     */
    @Transactional
    public AuthWithdrawRes withdraw(String userId, AuthWithdrawReq req) {
        AuthUser user = users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.isGuest()) throw new ApiException(ErrorCode.GUEST_FORBIDDEN);

        requireCurrentPassword(user, req.password());

        user.markWithdrawn(OffsetDateTime.now());
        return new AuthWithdrawRes(true);
    }

    /** 두 곳이 같은 검사를 씁니다. 실패 문구를 하나로 두어야 갈라지지 않습니다 */
    private void requireCurrentPassword(AuthUser user, String raw) {
        if (raw == null || user.passwordHash() == null || !ENCODER.matches(raw, user.passwordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Transactional
    public AuthProfileRes updateProfile(String userId, AuthProfileReq req) {
        if (!req.nicknameProvided() && !req.emailProvided()) {
            throw new ApiException(ErrorCode.NO_FIELDS);
        }
        AuthUser user = users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.isGuest()) throw new ApiException(ErrorCode.GUEST_FORBIDDEN);
        if (req.nicknameProvided() && !validNickname(req.nickname())) {
            throw invalidProfile();
        }
        if (req.emailProvided() && req.email() != null && !validEmail(req.email())) {
            throw invalidProfile();
        }
        if (req.emailProvided() && req.email() != null && !Objects.equals(req.email(), user.email())
                && users.findByEmailAndDeletedAtIsNull(req.email()).isPresent()) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        List<String> updated = new ArrayList<>(2);
        if (req.nicknameProvided() && !Objects.equals(req.nickname(), user.nickname())) {
            user.updateNickname(req.nickname());
            updated.add("nickname");
        }
        if (req.emailProvided() && !Objects.equals(req.email(), user.email())) {
            user.updateEmail(req.email());
            updated.add("email");
        }
        return new AuthProfileRes(user.id(), "member", user.nickname(), user.email(), updated);
    }

    /**
     * 필터가 부릅니다.
     *
     * @return 사용자 번호. <b>토큰이 틀렸거나 만료됐거나 지운 사용자면 {@code null}</b>
     */
    @Transactional(readOnly = true)
    public String resolveUserId(String token) {
        String userId = tokens.verify(token);
        if (userId == null) return null;
        return users.findByIdAndDeletedAtIsNull(userId).map(AuthUser::id).orElse(null);
    }

    private AuthUser guestFrom(String token) {
        String userId = tokens.verify(token);
        if (userId == null) return null;
        return users.findByIdAndDeletedAtIsNull(userId).filter(AuthUser::isGuest).orElse(null);
    }

    private static boolean validEmail(String email) {
        return email != null && email.length() <= 255
                && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private static boolean validPassword(String password) {
        return password != null && password.length() >= 8 && password.length() <= 64;
    }

    private static boolean validNickname(String nickname) {
        return nickname != null && !nickname.isBlank() && nickname.length() <= 20;
    }

    private static ApiException invalidProfile() {
        return new ApiException(ErrorCode.VALIDATION_FAILED, "닉네임 또는 이메일 형식이 올바르지 않습니다");
    }
}
