package com.youin.now.common.id;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * ID 생성기 — <b>접두어 + ULID</b>.
 *
 * <p>DB 설계서의 공통 규약입니다. 예 — {@code us_01H8X…} {@code ev_01H8X…}
 *
 * <p><b>왜 UUID 가 아니라 ULID 인가</b>
 * <ul>
 *   <li><b>정렬됩니다.</b> 앞 10글자가 밀리초 타임스탬프라 문자열 정렬 = 생성 순서입니다.
 *       인덱스가 뒤쪽에만 쌓여 삽입이 빠릅니다</li>
 *   <li><b>노출해도 안전합니다.</b> 순번이 아니라 남의 것을 넘겨짚을 수 없습니다</li>
 * </ul>
 *
 * <p><b>라이브러리를 쓰지 않았습니다.</b> 동결 사흘 전에 의존성을 늘리면
 * 다른 사람 빌드가 깨질 위험이 있고, 이 정도는 표준 라이브러리로 충분합니다.
 */
public final class Ids {

    private Ids() {}

    /** Crockford Base32 — I · L · O · U 를 뺀 32글자. 사람이 옮겨 적을 때 헷갈리지 않습니다. */
    private static final char[] B32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final SecureRandom RND = new SecureRandom();

    public static String user()       { return of("us"); }
    public static String session()    { return of("se"); }
    public static String userItem()   { return of("ui"); }
    public static String checkin()    { return of("ck"); }
    public static String evaluation() { return of("ev"); }
    public static String result()     { return of("er"); }
    public static String action()     { return of("ac"); }
    public static String log()        { return of("lg"); }

    /** @param prefix 밑줄 앞에 붙는 두세 글자 */
    public static String of(String prefix) {
        return prefix + "_" + ulid();
    }

    /** 26글자 ULID — 앞 10글자가 시각(48비트), 뒤 16글자가 무작위(80비트). */
    public static String ulid() {
        long time = Instant.now().toEpochMilli();
        char[] out = new char[26];

        // 시각 48비트를 10글자로
        for (int i = 9; i >= 0; i--) {
            out[i] = B32[(int) (time & 31)];
            time >>>= 5;
        }
        // 무작위 80비트를 16글자로
        byte[] rnd = new byte[10];
        RND.nextBytes(rnd);
        long hi = 0;
        for (int i = 0; i < 5; i++) hi = (hi << 8) | (rnd[i] & 0xFF);      // 40비트
        long lo = 0;
        for (int i = 5; i < 10; i++) lo = (lo << 8) | (rnd[i] & 0xFF);     // 40비트
        for (int i = 17; i >= 10; i--) { out[i] = B32[(int) (hi & 31)]; hi >>>= 5; }
        for (int i = 25; i >= 18; i--) { out[i] = B32[(int) (lo & 31)]; lo >>>= 5; }

        return new String(out);
    }
}
