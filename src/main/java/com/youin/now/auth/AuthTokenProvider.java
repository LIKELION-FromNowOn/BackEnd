package com.youin.now.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * JWT 발급과 검증 — <b>HS256</b>.
 *
 * <p>API 명세서의 응답 예시가 이미 {@code eyJhbGciOiJIUzI1NiJ9…} 이고,
 * 이것을 Base64 로 풀면 {@code {"alg":"HS256"}} 입니다.
 * <b>즉 명세서가 처음부터 HS256 JWT 를 전제하고 있었습니다.</b>
 *
 * <p><b>라이브러리를 쓰지 않은 이유</b> — 동결 사흘 전에 의존성을 추가하면
 * 세 사람의 빌드가 한 번에 깨질 수 있습니다. HS256 서명·검증은 표준 라이브러리로 충분하고,
 * 이 파일은 스프링 없이 그대로 돌려서 확인할 수 있습니다
 * ({@code src/test/.../AuthTokenProviderCheck.java}).
 *
 * <p><b>토큰에는 사용자 번호와 만료만 담습니다.</b> 이메일·닉네임을 넣지 않습니다 —
 * JWT 는 서명만 되어 있고 <b>암호화되어 있지 않아 누구나 내용을 읽을 수 있습니다.</b>
 */
public final class AuthTokenProvider {

    /**
     * {@code {"alg":"HS256"}} 를 Base64URL 로 미리 굳혀 둔 것 = {@code eyJhbGciOiJIUzI1NiJ9}
     *
     * <p><b>명세서 응답 예시와 글자 하나까지 같습니다.</b> {@code typ} 은 넣지 않았습니다 —
     * JWT 규격에서 선택 항목이고, 넣으면 헤더가 달라져 명세서 예시와 어긋납니다.
     */
    private static final String HEADER =
            b64("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));

    private final byte[] secret;
    private final long validSeconds;

    /**
     * @param secret       서명 키. <b>32바이트 이상.</b> 코드에 박지 말고 환경 변수로 주입하십시오
     * @param validSeconds 유효 기간(초). 게스트는 30일 정도가 무난합니다
     */
    public AuthTokenProvider(String secret, long validSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("서명 키는 32바이트 이상이어야 합니다");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.validSeconds = validSeconds;
    }

    /** 발급 결과. {@code expiresAt} 은 응답에 그대로 실립니다. */
    public record Issued(String token, Instant expiresAt) {}

    public Issued issue(String userId) {
        Instant exp = Instant.now().plusSeconds(validSeconds);
        String payload = b64(("{\"sub\":\"" + escape(userId) + "\",\"exp\":" + exp.getEpochSecond() + "}")
                .getBytes(StandardCharsets.UTF_8));
        String signing = HEADER + "." + payload;
        return new Issued(signing + "." + sign(signing), exp);
    }

    /**
     * @return 사용자 번호. <b>서명이 틀렸거나 만료됐으면 {@code null}</b> — 예외를 던지지 않습니다
     */
    public String verify(String token) {
        if (token == null) return null;
        String[] p = token.split("\\.");
        if (p.length != 3) return null;

        String signing = p[0] + "." + p[1];
        if (!constantTimeEquals(sign(signing), p[2])) return null;   // 서명 위조

        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Long exp = readLong(json, "exp");
        if (exp == null || Instant.now().getEpochSecond() >= exp) return null;   // 만료

        return readString(json, "sub");
    }

    // ── 내부 ────────────────────────────────────────────────

    private String sign(String signing) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return b64(mac.doFinal(signing.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("토큰 서명에 실패했습니다", e);
        }
    }

    private static String b64(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    /** 길이·내용 비교 시간을 일정하게 유지합니다. 서명 비교에서 타이밍 정보를 흘리지 않기 위한 것입니다. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readString(String json, String key) {
        String k = "\"" + key + "\":\"";
        int i = json.indexOf(k);
        if (i < 0) return null;
        int s = i + k.length(), e = json.indexOf('"', s);
        return (e < 0) ? null : json.substring(s, e);
    }

    private static Long readLong(String json, String key) {
        String k = "\"" + key + "\":";
        int i = json.indexOf(k);
        if (i < 0) return null;
        int s = i + k.length(), e = s;
        while (e < json.length() && (Character.isDigit(json.charAt(e)))) e++;
        try { return Long.parseLong(json.substring(s, e)); }
        catch (RuntimeException ex) { return null; }
    }
}
