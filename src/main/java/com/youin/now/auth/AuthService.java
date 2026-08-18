package com.youin.now.auth;

import com.youin.now.common.id.Ids;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게스트 발급과 토큰 검증.
 *
 * <p><b>게스트도 기능 제한이 없습니다.</b> 진입 장벽을 낮추기 위한 결정입니다(명세서 {@code NOW-AUTH-001}).
 */
@Service
public class AuthService {

    /** 날짜 경계가 KST 자정이라 만료 시각도 KST 로 내려 줍니다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AuthUserRepository users;
    private final AuthTokenProvider tokens;

    public AuthService(AuthUserRepository users,
                       @Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.valid-seconds:2592000}") long validSeconds) {
        this.users = users;
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
}
