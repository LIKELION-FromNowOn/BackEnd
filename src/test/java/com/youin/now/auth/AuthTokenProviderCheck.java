package com.youin.now.auth;

import com.youin.now.common.id.Ids;
import java.util.HashSet;
import java.util.Set;

/**
 * 토큰 발급·검증과 ID 생성기 <b>동작 확인</b>.
 *
 * <p>스프링도 DB 도 없이 {@code main} 으로 돕니다.
 * {@code SubtractPipelineCheck} 와 같은 방식이고, JUnit 으로 옮기지 않습니다
 * (한글 경로에서 {@code :test} 가 막히는 문제 — {@code .agent/REQUESTS.md} #4).
 */
public final class AuthTokenProviderCheck {

    static int pass = 0, fail = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) { pass++; System.out.println("  PASS  " + name); }
        else    { fail++; System.out.println("  FAIL  " + name + "  → " + detail); }
    }

    static final String SECRET = "이것은_최소_32바이트를_넘는_테스트용_서명키입니다";

    public static void main(String[] args) throws Exception {
        System.out.println("인증 동작 확인");
        System.out.println("=".repeat(64));

        // ── 1. ID 생성기 ─────────────────────────────────
        System.out.println("\n[1] ID 생성기");
        String id = Ids.user();
        check("접두어 + 26글자 ULID", id.startsWith("us_") && id.length() == 3 + 26, id);
        check("Crockford Base32 만 쓴다",
                id.substring(3).matches("[0-9ABCDEFGHJKMNPQRSTVWXYZ]{26}"), id);

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20000; i++) seen.add(Ids.ulid());
        check("2만 개를 뽑아도 겹치지 않는다", seen.size() == 20000, "중복 " + (20000 - seen.size()));

        String a = Ids.ulid();
        Thread.sleep(3);
        String b = Ids.ulid();
        check("나중에 만든 것이 문자열 정렬에서 뒤에 온다", a.compareTo(b) < 0, a + " vs " + b);

        // ── 2. 토큰 발급 ─────────────────────────────────
        System.out.println("\n[2] 토큰 발급");
        AuthTokenProvider p = new AuthTokenProvider(SECRET, 60);
        String userId = Ids.user();
        AuthTokenProvider.Issued issued = p.issue(userId);

        check("점 두 개로 나뉜 세 도막", issued.token().split("\\.").length == 3, issued.token());
        check("헤더가 명세서 예시와 글자까지 같다 (eyJhbGciOiJIUzI1NiJ9)",
                issued.token().startsWith("eyJhbGciOiJIUzI1NiJ9."), issued.token());
        check("만료가 미래다", issued.expiresAt().isAfter(java.time.Instant.now()), "" + issued.expiresAt());

        // ── 3. 검증 ─────────────────────────────────────
        System.out.println("\n[3] 검증");
        check("발급한 토큰에서 사용자 번호가 그대로 나온다",
                userId.equals(p.verify(issued.token())), String.valueOf(p.verify(issued.token())));

        // ── 4. 위조·오류를 막는가 (전부 null 이어야 정상) ──
        System.out.println("\n[4] 위조와 오류 — 전부 막혀야 정상");
        String t = issued.token();
        String[] head = t.split("\\.");

        check("서명을 한 글자 바꾸면 거부",
                p.verify(head[0] + "." + head[1] + "." + flip(head[2])) == null, "통과해 버림");
        check("내용을 바꾸면 거부 (다른 사람 번호로 위조)",
                p.verify(head[0] + "." + forge(userId) + "." + head[2]) == null, "통과해 버림");
        check("다른 서명키로 만든 토큰은 거부",
                p.verify(new AuthTokenProvider(SECRET.replace("테스트", "다른키"), 60)
                        .issue(userId).token()) == null, "통과해 버림");
        check("도막이 모자라면 거부", p.verify("abc.def") == null, "통과해 버림");
        check("아무 문자열이나 넣으면 거부", p.verify("그냥문자열") == null, "통과해 버림");
        check("null 을 넣어도 예외를 던지지 않고 거부", p.verify(null) == null, "예외가 남");

        // ── 5. 만료 ─────────────────────────────────────
        System.out.println("\n[5] 만료");
        AuthTokenProvider expired = new AuthTokenProvider(SECRET, 0);
        String old = expired.issue(userId).token();
        Thread.sleep(1100);
        check("만료된 토큰은 거부", expired.verify(old) == null, "통과해 버림");

        // ── 6. 서명키 길이 ───────────────────────────────
        System.out.println("\n[6] 서명키");
        boolean threw = false;
        try { new AuthTokenProvider("짧은키", 60); } catch (IllegalArgumentException e) { threw = true; }
        check("32바이트 미만 서명키는 시작할 때 막는다", threw, "그냥 만들어짐");

        // ── 7. 토큰에 개인정보가 없는가 ───────────────────
        System.out.println("\n[7] 토큰 내용");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(head[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        check("사용자 번호와 만료만 담겨 있다 (JWT 는 암호화가 아닙니다)",
                payload.contains("\"sub\"") && payload.contains("\"exp\"")
                        && !payload.contains("email") && !payload.contains("nickname"), payload);

        System.out.println("\n" + "=".repeat(64));
        System.out.printf("통과 %d · 실패 %d%n", pass, fail);
        if (fail > 0) System.exit(1);
    }

    static String flip(String s) {
        char c = s.charAt(0);
        return (c == 'A' ? 'B' : 'A') + s.substring(1);
    }

    static String forge(String realId) {
        String json = "{\"sub\":\"" + realId.replace("us_", "us_XXXX") + "\",\"exp\":9999999999}";
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
